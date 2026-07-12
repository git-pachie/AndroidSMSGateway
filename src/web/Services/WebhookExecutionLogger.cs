using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;
using Microsoft.Extensions.Options;
using Web.Options;

namespace Web.Services;

public sealed class WebhookExecutionLogger
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
    };

    private readonly SemaphoreSlim _mutex = new(1, 1);
    private readonly string _logFilePath;
    private readonly string _databasePath;
    private bool _databaseInitialized;

    public WebhookExecutionLogger(IHostEnvironment environment, IOptions<WebhookOptions> options)
    {
        var configuredPath = options.Value.LogFilePath.Trim();
        var configuredDatabasePath = options.Value.DatabasePath.Trim();
        _logFilePath = Path.IsPathRooted(configuredPath)
            ? configuredPath
            : Path.Combine(environment.ContentRootPath, configuredPath);
        _databasePath = Path.IsPathRooted(configuredDatabasePath)
            ? configuredDatabasePath
            : Path.Combine(environment.ContentRootPath, configuredDatabasePath);
    }

    public async Task LogAsync(WebhookExecutionLogEntry entry)
    {
        var directory = Path.GetDirectoryName(_logFilePath);
        if (!string.IsNullOrWhiteSpace(directory))
        {
            Directory.CreateDirectory(directory);
        }

        var payload = JsonSerializer.Serialize(entry, JsonOptions);
        var content = $"{payload}{Environment.NewLine}{new string('-', 80)}{Environment.NewLine}";

        await _mutex.WaitAsync();
        try
        {
            await EnsureDatabaseCreatedAsync();
            await File.AppendAllTextAsync(_logFilePath, content, Encoding.UTF8);
            await InsertIntoDatabaseAsync(entry);
        }
        finally
        {
            _mutex.Release();
        }
    }

    private async Task EnsureDatabaseCreatedAsync()
    {
        if (_databaseInitialized)
        {
            return;
        }

        var directory = Path.GetDirectoryName(_databasePath);
        if (!string.IsNullOrWhiteSpace(directory))
        {
            Directory.CreateDirectory(directory);
        }

        await using var connection = new SqliteConnection($"Data Source={_databasePath}");
        await connection.OpenAsync();

        var command = connection.CreateCommand();
        command.CommandText =
            """
            CREATE TABLE IF NOT EXISTS WebhookExecutions (
                Id INTEGER PRIMARY KEY AUTOINCREMENT,
                OccurredAtUtc TEXT NOT NULL,
                Method TEXT NOT NULL,
                Url TEXT NOT NULL,
                RequestId TEXT NULL,
                RemoteIp TEXT NULL,
                EventType TEXT NULL,
                DeviceId TEXT NULL,
                AuthorizationRequired INTEGER NOT NULL,
                Authorized INTEGER NOT NULL,
                HeadersJson TEXT NOT NULL,
                BodyJson TEXT NULL,
                BodyText TEXT NULL
            );
            """;
        await command.ExecuteNonQueryAsync();
        _databaseInitialized = true;
    }

    private async Task InsertIntoDatabaseAsync(WebhookExecutionLogEntry entry)
    {
        await using var connection = new SqliteConnection($"Data Source={_databasePath}");
        await connection.OpenAsync();

        var command = connection.CreateCommand();
        command.CommandText =
            """
            INSERT INTO WebhookExecutions (
                OccurredAtUtc,
                Method,
                Url,
                RequestId,
                RemoteIp,
                EventType,
                DeviceId,
                AuthorizationRequired,
                Authorized,
                HeadersJson,
                BodyJson,
                BodyText
            ) VALUES (
                $occurredAtUtc,
                $method,
                $url,
                $requestId,
                $remoteIp,
                $eventType,
                $deviceId,
                $authorizationRequired,
                $authorized,
                $headersJson,
                $bodyJson,
                $bodyText
            );
            """;
        command.Parameters.AddWithValue("$occurredAtUtc", entry.OccurredAtUtc.ToString("O"));
        command.Parameters.AddWithValue("$method", entry.Method);
        command.Parameters.AddWithValue("$url", entry.Url);
        command.Parameters.AddWithValue("$requestId", (object?)entry.RequestId ?? DBNull.Value);
        command.Parameters.AddWithValue("$remoteIp", (object?)entry.RemoteIp ?? DBNull.Value);
        command.Parameters.AddWithValue("$eventType", (object?)entry.EventType ?? DBNull.Value);
        command.Parameters.AddWithValue("$deviceId", (object?)entry.DeviceId ?? DBNull.Value);
        command.Parameters.AddWithValue("$authorizationRequired", entry.AuthorizationRequired ? 1 : 0);
        command.Parameters.AddWithValue("$authorized", entry.Authorized ? 1 : 0);
        command.Parameters.AddWithValue("$headersJson", JsonSerializer.Serialize(entry.Headers, JsonOptions));
        command.Parameters.AddWithValue(
            "$bodyJson",
            entry.Body is JsonElement body ? body.GetRawText() : DBNull.Value);
        command.Parameters.AddWithValue("$bodyText", (object?)entry.BodyText ?? DBNull.Value);

        await command.ExecuteNonQueryAsync();
    }
}
