namespace PersonnelMS.Messaging;

/// <summary>
/// Événement reçu depuis auth.exchange quand ms-auth crée un utilisateur.
/// Doit correspondre exactement à AuthEventMessage de ms-auth (camelCase JSON).
/// </summary>
public class UserCreatedEvent
{
    public string?   EventType { get; set; }   // USER_CREATED | USER_LOGGED_IN
    public long      UserId    { get; set; }
    public string?   Email     { get; set; }
    public string?   Role      { get; set; }   // MEDECIN | INFIRMIER | ADMIN | PATIENT
    public string?   Nom       { get; set; }
    public string?   Prenom    { get; set; }
    public long?     PatientId { get; set; }
    public DateTime  Timestamp { get; set; }
}
