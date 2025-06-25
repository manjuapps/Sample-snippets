// Smart cropping with multiple focal point detection methods

class SmartCropper {
  constructor() {
    this.canvas = document.createElement('canvas');
    this.ctx = this.canvas.getContext('2d');
  }

  // Method 1: Edge density detection (simple but effective)
  findEdgeDensityFocalPoint(imageData, width, height) {
    const data = imageData.data;
    const gridSize = 32; // Divide image into 32x32 pixel blocks
    const scores = [];
    
    for (let y = 0; y < height - gridSize; y += gridSize) {
      for (let x = 0; x < width - gridSize; x += gridSize) {
        let edgeScore = 0;
        
        // Calculate edge density in this block
        for (let dy = 0; dy < gridSize - 1; dy++) {
          for (let dx = 0; dx < gridSize - 1; dx++) {
            const idx = ((y + dy) * width + (x + dx)) * 4;
            const nextIdx = ((y + dy) * width + (x + dx + 1)) * 4;
            const belowIdx = ((y + dy + 1) * width + (x + dx)) * 4;
            
            // Simple edge detection using brightness differences
            const current = (data[idx] + data[idx + 1] + data[idx + 2]) / 3;
            const right = (data[nextIdx] + data[nextIdx + 1] + data[nextIdx + 2]) / 3;
            const below = (data[belowIdx] + data[belowIdx + 1] + data[belowIdx + 2]) / 3;
            
            edgeScore += Math.abs(current - right) + Math.abs(current - below);
          }
        }
        
        scores.push({
          x: x + gridSize / 2,
          y: y + gridSize / 2,
          score: edgeScore
        });
      }
    }
    
    // Return point with highest edge density
    return scores.reduce((best, current) => 
      current.score > best.score ? current : best
    );
  }

  // Method 2: Face detection using basic skin tone detection
  findFaceFocalPoint(imageData, width, height) {
    const data = imageData.data;
    const faceRegions = [];
    const blockSize = 20;
    
    for (let y = 0; y < height - blockSize; y += blockSize) {
      for (let x = 0; x < width - blockSize; x += blockSize) {
        let skinPixels = 0;
        let totalPixels = 0;
        
        for (let dy = 0; dy < blockSize; dy++) {
          for (let dx = 0; dx < blockSize; dx++) {
            const idx = ((y + dy) * width + (x + dx)) * 4;
            const r = data[idx];
            const g = data[idx + 1];
            const b = data[idx + 2];
            
            // Simple skin tone detection
            if (this.isSkinTone(r, g, b)) {
              skinPixels++;
            }
            totalPixels++;
          }
        }
        
        const skinRatio = skinPixels / totalPixels;
        if (skinRatio > 0.3) { // 30% skin tone threshold
          faceRegions.push({
            x: x + blockSize / 2,
            y: y + blockSize / 2,
            score: skinRatio,
            area: skinPixels
          });
        }
      }
    }
    
    if (faceRegions.length > 0) {
      // Return largest skin tone region
      return faceRegions.reduce((best, current) => 
        current.area > best.area ? current : best
      );
    }
    
    return null;
  }

  // Simple skin tone detection
  isSkinTone(r, g, b) {
    return r > 95 && g > 40 && b > 20 && 
           r > g && r > b && 
           Math.abs(r - g) > 15 && 
           r - b > 15;
  }

  // Method 3: Rule of thirds focal points
  getRuleOfThirdsFocalPoints(width, height) {
    return [
      { x: width / 3, y: height / 3, name: 'top-left' },
      { x: (2 * width) / 3, y: height / 3, name: 'top-right' },
      { x: width / 3, y: (2 * height) / 3, name: 'bottom-left' },
      { x: (2 * width) / 3, y: (2 * height) / 3, name: 'bottom-right' }
    ];
  }

  // Method 4: Contrast-based focal point
  findContrastFocalPoint(imageData, width, height) {
    const data = imageData.data;
    const blockSize = 40;
    const scores = [];
    
    for (let y = 0; y < height - blockSize; y += blockSize) {
      for (let x = 0; x < width - blockSize; x += blockSize) {
        let minBrightness = 255;
        let maxBrightness = 0;
        
        for (let dy = 0; dy < blockSize; dy++) {
          for (let dx = 0; dx < blockSize; dx++) {
            const idx = ((y + dy) * width + (x + dx)) * 4;
            const brightness = (data[idx] + data[idx + 1] + data[idx + 2]) / 3;
            minBrightness = Math.min(minBrightness, brightness);
            maxBrightness = Math.max(maxBrightness, brightness);
          }
        }
        
        const contrast = maxBrightness - minBrightness;
        scores.push({
          x: x + blockSize / 2,
          y: y + blockSize / 2,
          score: contrast
        });
      }
    }
    
    return scores.reduce((best, current) => 
      current.score > best.score ? current : best
    );
  }

  // Main smart crop function
  async smartCrop(imageElement, targetAspectRatio, outputWidth = 800, method = 'auto') {
    const originalWidth = imageElement.naturalWidth;
    const originalHeight = imageElement.naturalHeight;
    
    // Draw image to canvas to get image data
    this.canvas.width = originalWidth;
    this.canvas.height = originalHeight;
    this.ctx.drawImage(imageElement, 0, 0);
    
    const imageData = this.ctx.getImageData(0, 0, originalWidth, originalHeight);
    
    let focalPoint;
    
    switch (method) {
      case 'face':
        focalPoint = this.findFaceFocalPoint(imageData, originalWidth, originalHeight);
        break;
      case 'edge':
        focalPoint = this.findEdgeDensityFocalPoint(imageData, originalWidth, originalHeight);
        break;
      case 'contrast':
        focalPoint = this.findContrastFocalPoint(imageData, originalWidth, originalHeight);
        break;
      case 'auto':
      default:
        // Try face detection first, then edge density
        focalPoint = this.findFaceFocalPoint(imageData, originalWidth, originalHeight) ||
                    this.findEdgeDensityFocalPoint(imageData, originalWidth, originalHeight);
        break;
    }
    
    // Calculate crop dimensions
    const cropDimensions = this.calculateSmartCrop(
      originalWidth, originalHeight, targetAspectRatio, focalPoint
    );
    
    // Create final cropped canvas
    const finalWidth = outputWidth;
    const finalHeight = finalWidth / targetAspectRatio;
    
    const resultCanvas = document.createElement('canvas');
    const resultCtx = resultCanvas.getContext('2d');
    resultCanvas.width = finalWidth;
    resultCanvas.height = finalHeight;
    
    resultCtx.drawImage(
      imageElement,
      cropDimensions.x, cropDimensions.y, cropDimensions.width, cropDimensions.height,
      0, 0, finalWidth, finalHeight
    );
    
    return {
      canvas: resultCanvas,
      focalPoint: focalPoint,
      cropArea: cropDimensions,
      method: method
    };
  }

  // Calculate crop area based on focal point
  calculateSmartCrop(originalWidth, originalHeight, targetAspectRatio, focalPoint) {
    const originalAspectRatio = originalWidth / originalHeight;
    
    let cropWidth, cropHeight;
    
    if (originalAspectRatio > targetAspectRatio) {
      cropHeight = originalHeight;
      cropWidth = cropHeight * targetAspectRatio;
    } else {
      cropWidth = originalWidth;
      cropHeight = cropWidth / targetAspectRatio;
    }
    
    let cropX, cropY;
    
    if (focalPoint) {
      // Center crop around focal point, but keep within bounds
      cropX = Math.max(0, Math.min(
        originalWidth - cropWidth,
        focalPoint.x - cropWidth / 2
      ));
      cropY = Math.max(0, Math.min(
        originalHeight - cropHeight,
        focalPoint.y - cropHeight / 2
      ));
    } else {
      // Fall back to center crop
      cropX = (originalWidth - cropWidth) / 2;
      cropY = (originalHeight - cropHeight) / 2;
    }
    
    return { x: cropX, y: cropY, width: cropWidth, height: cropHeight };
  }
}

// Usage examples
const smartCropper = new SmartCropper();

// Example 1: Auto smart crop (tries face detection, then edge density)
async function cropImageSmart(imageElement) {
  const result = await smartCropper.smartCrop(imageElement, 16/9, 1200, 'auto');
  document.body.appendChild(result.canvas);
  console.log('Focal point found:', result.focalPoint);
}

// Example 2: Specific method
async function cropWithFaceDetection(imageElement) {
  const result = await smartCropper.smartCrop(imageElement, 1/1, 500, 'face');
  return result;
}

// Example 3: File upload with smart cropping
async function handleSmartCropUpload(file) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = async () => {
      try {
        const result = await smartCropper.smartCrop(img, 4/3, 800, 'auto');
        resolve(result);
      } catch (error) {
        reject(error);
      }
    };
    img.onerror = () => reject(new Error('Failed to load image'));
    img.src = URL.createObjectURL(file);
  });
}

// Example 4: Compare different methods
async function compareSmartCropMethods(imageElement) {
  const methods = ['face', 'edge', 'contrast'];
  const results = [];
  
  for (const method of methods) {
    const result = await smartCropper.smartCrop(imageElement, 16/9, 400, method);
    results.push({
      method: method,
      canvas: result.canvas,
      focalPoint: result.focalPoint
    });
  }
  
  return results;
}
