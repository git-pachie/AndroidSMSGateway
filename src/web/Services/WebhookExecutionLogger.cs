using System.Text;
using System.Text.Json;
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

    public WebhookExecutionLogger(IHostEnvironment environment, IOptions<WebhookOptions> options)
    {
        var configuredPath = options.Value.LogFilePath.Trim();
        _logFilePath = Path.IsPathRooted(configuredPath)
            ? configuredPath
            : Path.Combine(environment.ContentRootPath, configuredPath);
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
            await File.AppendAllTextAsync(_logFilePath, content, Encoding.UTF8);
        }
        finally
        {
            _mutex.Release();
        }
    }
}
