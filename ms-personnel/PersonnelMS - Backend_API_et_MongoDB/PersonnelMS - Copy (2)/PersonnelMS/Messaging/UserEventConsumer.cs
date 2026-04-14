using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Options;
using MongoDB.Driver;
using PersonnelMS.Models;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace PersonnelMS.Messaging;

/// <summary>
/// Service de fond qui consomme les événements RabbitMQ depuis auth.exchange.
///
/// <para>Quand ms-auth crée un compte avec le rôle MEDECIN ou INFIRMIER,
/// ce service crée automatiquement la fiche personnel correspondante
/// dans MongoDB (si elle n'existe pas déjà).</para>
/// </summary>
public class UserEventConsumer : BackgroundService
{
    private readonly RabbitMQSettings     _settings;
    private readonly IMongoDatabase       _database;
    private readonly ILogger<UserEventConsumer> _logger;

    private IConnection? _connection;
    private IModel?      _channel;

    private const string QueueName = "personnel.auth.queue";

    public UserEventConsumer(
        IOptions<RabbitMQSettings> settings,
        IMongoDatabase database,
        ILogger<UserEventConsumer> logger)
    {
        _settings = settings.Value;
        _database = database;
        _logger   = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        // Tentatives de connexion avec backoff exponentiel
        for (int attempt = 1; attempt <= 5; attempt++)
        {
            try
            {
                Connect();
                _logger.LogInformation("[RabbitMQ] Connecté à {Host}:{Port}", _settings.Host, _settings.Port);
                break;
            }
            catch (Exception ex)
            {
                int delay = (int)Math.Pow(2, attempt) * 1000;
                _logger.LogWarning("[RabbitMQ] Tentative {Attempt}/5 échouée: {Msg} – retry dans {Delay}ms",
                    attempt, ex.Message, delay);
                await Task.Delay(delay, stoppingToken);
            }
        }

        if (_channel == null || !_channel.IsOpen)
        {
            _logger.LogError("[RabbitMQ] Impossible de se connecter après 5 tentatives. Le consumer ne démarrera pas.");
            return;
        }

        var consumer = new EventingBasicConsumer(_channel);
        consumer.Received += async (_, ea) => await ProcessMessageAsync(ea);

        _channel.BasicConsume(queue: QueueName, autoAck: false, consumer: consumer);
        _logger.LogInformation("[RabbitMQ] En écoute sur la queue '{Queue}'", QueueName);

        // Maintien du service actif jusqu'à l'arrêt
        await Task.Delay(Timeout.Infinite, stoppingToken);
    }

    private void Connect()
    {
        var factory = new ConnectionFactory
        {
            HostName    = _settings.Host,
            Port        = _settings.Port,
            UserName    = _settings.Username,
            Password    = _settings.Password,
            VirtualHost = _settings.VirtualHost,
            DispatchConsumersAsync = false
        };

        _connection = factory.CreateConnection("ms-personnel");
        _channel    = _connection.CreateModel();

        // Déclarer l'exchange (idempotent)
        _channel.ExchangeDeclare(
            exchange: _settings.AuthExchange,
            type: "topic",
            durable: true,
            autoDelete: false);

        // Déclarer la queue dédiée à ms-personnel
        _channel.QueueDeclare(
            queue: QueueName,
            durable: true,
            exclusive: false,
            autoDelete: false);

        // Lier la queue à auth.exchange sur la routing key "user.created"
        _channel.QueueBind(
            queue: QueueName,
            exchange: _settings.AuthExchange,
            routingKey: "user.created");

        // Prefetch = 1 : traiter un message à la fois
        _channel.BasicQos(prefetchSize: 0, prefetchCount: 1, global: false);
    }

    private async Task ProcessMessageAsync(BasicDeliverEventArgs ea)
    {
        string body = Encoding.UTF8.GetString(ea.Body.ToArray());
        _logger.LogDebug("[RabbitMQ] Message reçu: {Body}", body);

        try
        {
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            var evt = JsonSerializer.Deserialize<UserCreatedEvent>(body, options);

            if (evt == null || evt.EventType != "USER_CREATED")
            {
                _channel!.BasicAck(ea.DeliveryTag, multiple: false);
                return;
            }

            await HandleUserCreatedAsync(evt);
            _channel!.BasicAck(ea.DeliveryTag, multiple: false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[RabbitMQ] Erreur traitement message: {Body}", body);
            // Nack sans requeue pour éviter la boucle infinie
            _channel!.BasicNack(ea.DeliveryTag, multiple: false, requeue: false);
        }
    }

    private async Task HandleUserCreatedAsync(UserCreatedEvent evt)
    {
        // Seuls les rôles MEDECIN et INFIRMIER ont une fiche dans ms-personnel
        if (evt.Role is not ("MEDECIN" or "INFIRMIER"))
        {
            _logger.LogDebug("[RabbitMQ] Rôle {Role} ignoré (pas personnel médical)", evt.Role);
            return;
        }

        var collection = _database.GetCollection<Personnel>("personnels");

        // Vérifier doublon sur le courriel
        var existing = await collection
            .Find(p => p.Courriel == evt.Email)
            .FirstOrDefaultAsync();

        if (existing != null)
        {
            _logger.LogWarning("[RabbitMQ] Personnel avec courriel {Email} existe déjà, ignoré", evt.Email);
            return;
        }

        var personnel = new Personnel
        {
            Nom          = evt.Nom    ?? "",
            Prenom       = evt.Prenom ?? "",
            Courriel     = evt.Email  ?? "",
            Matricule    = $"AUTH-{evt.UserId}",   // Matricule provisoire depuis userId
            Poste        = evt.Role   ?? "PERSONNEL",
            Statut       = Enums.Statut.Actif,
            DateEmbauche = DateTime.UtcNow,
        };

        await collection.InsertOneAsync(personnel);
        _logger.LogInformation("[RabbitMQ] Fiche personnel créée pour {Email} (rôle: {Role})",
            evt.Email, evt.Role);
    }

    public override void Dispose()
    {
        _channel?.Close();
        _connection?.Close();
        base.Dispose();
    }
}
