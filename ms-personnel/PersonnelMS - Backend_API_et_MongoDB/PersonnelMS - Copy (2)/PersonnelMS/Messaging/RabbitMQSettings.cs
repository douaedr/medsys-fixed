namespace PersonnelMS.Messaging;

/// <summary>
/// Paramètres de connexion RabbitMQ lus depuis appsettings.json / variables d'environnement.
/// </summary>
public class RabbitMQSettings
{
    public string Host        { get; set; } = "localhost";
    public int    Port        { get; set; } = 5672;
    public string Username    { get; set; } = "guest";
    public string Password    { get; set; } = "guest";
    public string VirtualHost { get; set; } = "/";

    // Exchange sur lequel ms-auth publie les événements utilisateur
    public string AuthExchange      { get; set; } = "auth.exchange";
    // Exchange sur lequel ms-personnel peut publier ses propres événements
    public string PersonnelExchange { get; set; } = "personnel.exchange";
}
