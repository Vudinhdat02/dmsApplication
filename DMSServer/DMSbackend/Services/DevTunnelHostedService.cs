// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

using System.Diagnostics;

namespace DMSbackend.Services;

/// <summary>
/// Starts the configured Microsoft Dev Tunnel with the backend during local development.
/// </summary>
public sealed class DevTunnelHostedService(
    IConfiguration configuration,
    IHostEnvironment environment,
    ILogger<DevTunnelHostedService> logger) : IHostedService
{
    private Process? _process;

    public Task StartAsync(CancellationToken cancellationToken)
    {
        if (!environment.IsDevelopment() ||
            !configuration.GetValue<bool>("DevTunnel:AutoStart"))
        {
            return Task.CompletedTask;
        }

        var tunnelId = configuration["DevTunnel:TunnelId"];
        if (string.IsNullOrWhiteSpace(tunnelId))
        {
            logger.LogWarning("Dev Tunnel auto-start is enabled, but DevTunnel:TunnelId is empty.");
            return Task.CompletedTask;
        }

        try
        {
            var startInfo = new ProcessStartInfo
            {
                FileName = ResolveExecutablePath(),
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            startInfo.ArgumentList.Add("host");
            startInfo.ArgumentList.Add(tunnelId);

            _process = new Process
            {
                StartInfo = startInfo,
                EnableRaisingEvents = true
            };
            _process.OutputDataReceived += (_, args) => LogOutput(args.Data, false);
            _process.ErrorDataReceived += (_, args) => LogOutput(args.Data, true);

            if (_process.Start())
            {
                _process.BeginOutputReadLine();
                _process.BeginErrorReadLine();
                logger.LogInformation("Starting Dev Tunnel {TunnelId}.", tunnelId);
            }
        }
        catch (Exception exception)
        {
            logger.LogError(exception,
                "Unable to start Dev Tunnel. Ensure the devtunnel CLI is installed and signed in.");
        }

        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        try
        {
            if (_process is { HasExited: false })
            {
                _process.Kill(entireProcessTree: true);
                _process.WaitForExit(5_000);
                logger.LogInformation("Stopped Dev Tunnel.");
            }
        }
        catch (Exception exception)
        {
            logger.LogWarning(exception, "Unable to stop the Dev Tunnel process cleanly.");
        }
        finally
        {
            _process?.Dispose();
        }

        return Task.CompletedTask;
    }

    private string ResolveExecutablePath()
    {
        var configuredPath = configuration["DevTunnel:ExecutablePath"];
        if (!string.IsNullOrWhiteSpace(configuredPath))
        {
            var expandedPath = Environment.ExpandEnvironmentVariables(configuredPath);
            if (File.Exists(expandedPath))
            {
                return expandedPath;
            }

            logger.LogWarning(
                "Configured Dev Tunnel executable was not found at {ExecutablePath}.",
                expandedPath);
        }

        var localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var wingetPackagesPath = Path.Combine(localAppData, "Microsoft", "WinGet", "Packages");

        try
        {
            if (Directory.Exists(wingetPackagesPath))
            {
                var wingetExecutable = Directory
                    .EnumerateFiles(wingetPackagesPath, "devtunnel.exe", SearchOption.AllDirectories)
                    .FirstOrDefault();
                if (wingetExecutable is not null)
                {
                    return wingetExecutable;
                }
            }
        }
        catch (Exception exception)
        {
            logger.LogDebug(exception, "Unable to search WinGet packages for devtunnel.exe.");
        }

        return "devtunnel";
    }

    private void LogOutput(string? message, bool isError)
    {
        if (string.IsNullOrWhiteSpace(message))
        {
            return;
        }

        if (isError)
        {
            logger.LogWarning("Dev Tunnel: {Message}", message);
        }
        else
        {
            logger.LogInformation("Dev Tunnel: {Message}", message);
        }
    }
}
