using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Http.Extensions;
using Microsoft.Extensions.Options;
using Web.Options;
using Web.Services;

var builder = WebApplication.CreateBuilder(args);

builder.WebHost.UseUrls(builder.Configuration["Server:Urls"] ?? "http://0.0.0.0:5111");

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.Configure<WebhookOptions>(builder.Configuration.GetSection(WebhookOptions.SectionName));
builder.Services.AddSingleton<WebhookExecutionLogger>();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();

app.MapGet("/health", () => Results.Ok(new
{
    status = "ok",
    service = "webhook-endpoint",
    time = DateTimeOffset.UtcNow,
}))
.WithName("GetHealth")
.WithSummary("Returns webhook API health status.")
.WithDescription("Use this endpoint to verify that the webhook service is running.");

app.MapPost("/webhook", async (
    HttpContext context,
    IOptions<WebhookOptions> options,
    WebhookExecutionLogger logger) =>
{
    context.Request.EnableBuffering();

    using var reader = new StreamReader(context.Request.Body, Encoding.UTF8, detectEncodingFromByteOrderMarks: false, leaveOpen: true);
    var body = await reader.ReadToEndAsync();
    context.Request.Body.Position = 0;

    var requestTime = DateTimeOffset.UtcNow;
    var authHeader = context.Request.Headers.Authorization.ToString();
    var expectedToken = options.Value.ExpectedBearerToken?.Trim();
    var authorizationRequired = !string.IsNullOrWhiteSpace(expectedToken);
    var authorized = !authorizationRequired || string.Equals(authHeader, $"Bearer {expectedToken}", StringComparison.Ordinal);
    var requestId = context.Request.Headers["X-Request-ID"].ToString();
    JsonElement? parsedBody = null;
    string? bodyText = null;

    if (!string.IsNullOrWhiteSpace(body))
    {
        try
        {
            parsedBody = JsonSerializer.Deserialize<JsonElement>(body);
        }
        catch (JsonException)
        {
            bodyText = body;
        }
    }

    await logger.LogAsync(new WebhookExecutionLogEntry(
        OccurredAtUtc: requestTime,
        Method: context.Request.Method,
        Url: context.Request.GetDisplayUrl(),
        RequestId: requestId,
        RemoteIp: context.Connection.RemoteIpAddress?.ToString(),
        EventType: context.Request.Headers["X-SMS-Gateway-Event"].ToString(),
        DeviceId: context.Request.Headers["X-SMS-Gateway-Device"].ToString(),
        AuthorizationRequired: authorizationRequired,
        Authorized: authorized,
        Headers: context.Request.Headers.ToDictionary(
            pair => pair.Key,
            pair => pair.Value.ToString()),
        Body: parsedBody,
        BodyText: bodyText));

    if (!authorized)
    {
        return Results.Unauthorized();
    }

    object payload;
    if (parsedBody is JsonElement jsonBody)
    {
        payload = jsonBody;
    }
    else if (string.IsNullOrWhiteSpace(body))
    {
        payload = new { };
    }
    else
    {
        return Results.BadRequest(new
        {
            success = false,
            error = "Request body must be valid JSON.",
        });
    }

    return Results.Ok(new
    {
        success = true,
        message = "Webhook received and logged.",
        requestId,
        receivedAtUtc = requestTime,
        payload,
    });
})
.WithName("ReceiveWebhook")
.WithSummary("Receives Android SMS Gateway webhook events.")
.WithDescription("Logs every webhook execution to a text file and optionally validates the Authorization bearer token.");

app.Run();
