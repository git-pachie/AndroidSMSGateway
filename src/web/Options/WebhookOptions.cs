namespace Web.Options;

public sealed class WebhookOptions
{
    public const string SectionName = "Webhook";

    public string LogFilePath { get; set; } = "logs/webhook-executions.log";

    public string? ExpectedBearerToken { get; set; }
}
