using FluentValidation;
using FluentValidation.AspNetCore;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using Serilog;
using System.Text;

Log.Logger = new LoggerConfiguration()
    .WriteTo.Console()
    .WriteTo.File("logs/hospital-.log", rollingInterval: RollingInterval.Day)
    .Enrich.FromLogContext()
    .CreateLogger();

var builder = WebApplication.CreateBuilder(args);
builder.Host.UseSerilog();

// ── MongoDB ───────────────────────────────────────────────────────────────────
builder.Services.Configure<PersonnelMS.Configurations.MongoSettings>(
    builder.Configuration.GetSection("MongoDB"));

builder.Services.AddSingleton(sp =>
{
    var settings = sp.GetRequiredService<Microsoft.Extensions.Options.IOptions<PersonnelMS.Configurations.MongoSettings>>().Value;
    return new MongoClient(settings.ConnectionURI);
});
builder.Services.AddScoped(sp =>
{
    var settings = sp.GetRequiredService<Microsoft.Extensions.Options.IOptions<PersonnelMS.Configurations.MongoSettings>>().Value;
    var client = sp.GetRequiredService<MongoClient>();
    return client.GetDatabase(settings.DatabaseName);
});

PersonnelMS.Configurations.MongoConfiguration.ConfigurerConventions();

// ── CORS ──────────────────────────────────────────────────────────────────────
var allowedOrigins = builder.Configuration
    .GetSection("Cors:AllowedOrigins").Get<string[]>()
    ?? new[] { "http://localhost:5173", "http://localhost:3000", "http://localhost:8080" };

builder.Services.AddCors(options =>
{
    options.AddPolicy("MedSysCors", policy =>
        policy.WithOrigins(allowedOrigins)
              .AllowAnyMethod()
              .AllowAnyHeader()
              .AllowCredentials());
});

// ── JWT Bearer ────────────────────────────────────────────────────────────────
var jwtSecret = builder.Configuration["Jwt:Secret"]
    ?? "medsys-hospital-jwt-secret-key-2026-very-long-and-secure-string-please-change-in-prod";
var jwtKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret));

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey         = jwtKey,
            ValidateIssuer           = false,
            ValidateAudience         = false,
            ValidateLifetime         = true,
            ClockSkew                = TimeSpan.FromSeconds(30)
        };
    });

builder.Services.AddAuthorization();

// ── Controllers + Validation ──────────────────────────────────────────────────
builder.Services.AddControllers()
    .AddFluentValidation(fv => fv.RegisterValidatorsFromAssemblyContaining<PersonnelMS.Validators.PersonnelCreateDtoValidator>())
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
    });

builder.Services.AddEndpointsApiExplorer();

// ── Swagger avec support JWT ───────────────────────────────────────────────────
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new Microsoft.OpenApi.Models.OpenApiInfo
    {
        Title   = "PersonnelMS API",
        Version = "v1",
        Description = "Gestion du personnel hospitalier — MedSys"
    });

    // Bouton Authorize dans Swagger UI
    options.AddSecurityDefinition("Bearer", new Microsoft.OpenApi.Models.OpenApiSecurityScheme
    {
        Name         = "Authorization",
        Type         = Microsoft.OpenApi.Models.SecuritySchemeType.Http,
        Scheme       = "bearer",
        BearerFormat = "JWT",
        In           = Microsoft.OpenApi.Models.ParameterLocation.Header,
        Description  = "Entrez votre token JWT (sans 'Bearer ')"
    });
    options.AddSecurityRequirement(new Microsoft.OpenApi.Models.OpenApiSecurityRequirement
    {
        {
            new Microsoft.OpenApi.Models.OpenApiSecurityScheme
            {
                Reference = new Microsoft.OpenApi.Models.OpenApiReference
                {
                    Type = Microsoft.OpenApi.Models.ReferenceType.SecurityScheme,
                    Id   = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });

    var xmlFile = System.IO.Path.ChangeExtension(
        System.Reflection.Assembly.GetExecutingAssembly().Location, ".xml");
    if (System.IO.File.Exists(xmlFile))
        options.IncludeXmlComments(xmlFile);
});

// ── Repositories ──────────────────────────────────────────────────────────────
builder.Services.AddScoped<PersonnelMS.Repositories.IPersonnelRepository, PersonnelMS.Repositories.PersonnelRepository>();
builder.Services.AddScoped<PersonnelMS.Repositories.IEquipeRepository, PersonnelMS.Repositories.EquipeRepository>();
builder.Services.AddScoped<PersonnelMS.Repositories.IDisponibiliteRepository, PersonnelMS.Repositories.DisponibiliteRepository>();
builder.Services.AddScoped<PersonnelMS.Repositories.IAbsenceRepository, PersonnelMS.Repositories.AbsenceRepository>();
builder.Services.AddScoped<PersonnelMS.Repositories.IDemandeModificationRepository, PersonnelMS.Repositories.DemandeModificationRepository>();
builder.Services.AddScoped<PersonnelMS.Repositories.IPlanningRepository, PersonnelMS.Repositories.PlanningRepository>();
builder.Services.AddScoped<PersonnelMS.Repositories.ICreneauRepository, PersonnelMS.Repositories.CreneauRepository>();

// ── Services ──────────────────────────────────────────────────────────────────
builder.Services.AddScoped<PersonnelMS.Services.IServicePersonnel, PersonnelMS.Services.ServicePersonnel>();
builder.Services.AddScoped<PersonnelMS.Services.IMedecinService, PersonnelMS.Services.MedecinService>();
builder.Services.AddScoped<PersonnelMS.Services.IInfirmierService, PersonnelMS.Services.InfirmierService>();
builder.Services.AddScoped<PersonnelMS.Services.IAideSoignantService, PersonnelMS.Services.AideSoignantService>();
builder.Services.AddScoped<PersonnelMS.Services.ISecretaireService, PersonnelMS.Services.SecretaireService>();
builder.Services.AddScoped<PersonnelMS.Services.IBrancardierService, PersonnelMS.Services.BrancardierService>();
builder.Services.AddScoped<PersonnelMS.Services.IDirecteurService, PersonnelMS.Services.DirecteurService>();
builder.Services.AddScoped<PersonnelMS.Services.IDisponibiliteService, PersonnelMS.Services.DisponibiliteService>();
builder.Services.AddScoped<PersonnelMS.Services.IAbsenceService, PersonnelMS.Services.AbsenceService>();
builder.Services.AddScoped<PersonnelMS.Services.IDemandeModificationService, PersonnelMS.Services.DemandeModificationService>();
builder.Services.AddScoped<PersonnelMS.Services.IEquipeService, PersonnelMS.Services.EquipeService>();
builder.Services.AddScoped<PersonnelMS.Services.IPlanningService, PersonnelMS.Services.PlanningService>();
builder.Services.AddScoped<PersonnelMS.Services.ICreneauService, PersonnelMS.Services.CreneauService>();
builder.Services.AddScoped<PersonnelMS.Services.IRapportService, PersonnelMS.Services.RapportService>();

// ── Build ─────────────────────────────────────────────────────────────────────
var app = builder.Build();

// Création des indexes MongoDB au démarrage
using (var scope = app.Services.CreateScope())
{
    var database = scope.ServiceProvider.GetRequiredService<IMongoDatabase>();
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesPersonnelsAsync(database);
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesCreneauxAsync(database);
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesPlanningsAsync(database);
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesDisponibilitesAsync(database);
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesAbsencesAsync(database);
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesDemandesModificationAsync(database);
    await PersonnelMS.Configurations.MongoConfiguration.CreerIndexesEquipesAsync(database);
}

// ── Middleware pipeline ───────────────────────────────────────────────────────
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "PersonnelMS v1");
        c.RoutePrefix = "swagger";
    });
}

app.UseMiddleware<PersonnelMS.Middleware.MiddlewareGestionExceptions>();

app.UseCors("MedSysCors");

// Désactiver HTTPS redirect en développement (gateway gère le SSL)
if (!app.Environment.IsDevelopment())
    app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
