Add-Type -AssemblyName System.Drawing

$inputPath = "D:\programFile\IDEA\IntelliJ IDEA 2025.2.1\code\0519\architecture_english_block.png"
$outputPath = "D:\programFile\IDEA\IntelliJ IDEA 2025.2.1\code\0519\architecture_english_16x9_perfect.png"

$img = [System.Drawing.Image]::FromFile($inputPath)

$origWidth = $img.Width
$origHeight = $img.Height

$targetRatio = 16.0 / 9.0
$currentRatio = $origWidth / $origHeight

if ($currentRatio -gt $targetRatio) {
    # wider than 16:9 -> pad height
    $newWidth = $origWidth
    $newHeight = [int][Math]::Round($newWidth / $targetRatio)
} else {
    # taller than 16:9 -> pad width
    $newHeight = $origHeight
    $newWidth = [int][Math]::Round($newHeight * $targetRatio)
}

$marginFactor = 1.05
$newWidth = [int][Math]::Round($newWidth * $marginFactor)
$newHeight = [int][Math]::Round($newHeight * $marginFactor)

$bmp = New-Object System.Drawing.Bitmap $newWidth, $newHeight
$graphics = [System.Drawing.Graphics]::FromImage($bmp)
$graphics.Clear([System.Drawing.Color]::White)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

$x = [int][Math]::Round(($newWidth - $origWidth) / 2)
$y = [int][Math]::Round(($newHeight - $origHeight) / 2)

$graphics.DrawImage($img, $x, $y, $origWidth, $origHeight)

$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$graphics.Dispose()
$bmp.Dispose()
$img.Dispose()

Write-Output "Image successfully padded to 16:9 aspect ratio and saved to $outputPath"
