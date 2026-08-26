// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

using DMSbackend.Models;
using DMSbackend.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace DMSbackend.Controllers;

[ApiController]
[Authorize]
[Route("api")]
public sealed class AiController(GroqService groq, ILogger<AiController> logger) : ControllerBase
{
    [HttpPost("analyze-driving")]
    [EnableRateLimiting("ai")]
    public async Task<ActionResult<TextResponse>> Analyze(
        [FromBody] DrivingAnalysisRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            return Ok(new TextResponse(await groq.AnalyzeAsync(request, cancellationToken)));
        }
        catch (InvalidOperationException exception)
        {
            logger.LogError(exception, "Groq chưa được cấu hình");
            return Problem("Dịch vụ AI chưa được cấu hình", statusCode: 503);
        }
        catch (HttpRequestException exception)
        {
            logger.LogWarning(exception, "Không thể gọi Groq");
            return Problem("Không thể kết nối dịch vụ AI", statusCode: 502);
        }
    }
}
