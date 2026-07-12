namespace Web.Services;

public sealed record WebhookExecutionLogEntry(
    DateTimeOffset OccurredAtUtc,
    string Method,
    string Url,
    string? RequestId,
    string? RemoteIp,
    string? EventType,
    string? DeviceId,
    bool AuthorizationRequired,
    bool Authorized,
    IReadOnlyDictionary<string, string> Headers,
    string Body);
