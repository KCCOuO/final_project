Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$Screen = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$Bitmap = New-Object System.Drawing.Bitmap $Screen.Width, $Screen.Height
$Graphics = [System.Drawing.Graphics]::FromImage($Bitmap)
$Graphics.CopyFromScreen($Screen.X, $Screen.Y, 0, 0, $Bitmap.Size)
$Bitmap.Save("C:\Users\user\.gemini\antigravity\brain\b92954a6-5f0a-4fce-a5b6-d397daa12fe8\MedicalML_Screenshot.png", [System.Drawing.Imaging.ImageFormat]::Png)
$Graphics.Dispose()
$Bitmap.Dispose()
