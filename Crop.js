// Function to calculate crop dimensions for center cropping
function calculateCenterCrop(originalWidth, originalHeight, targetAspectRatio) {
  const originalAspectRatio = originalWidth / originalHeight;
  
  let cropWidth, cropHeight;
  
  if (originalAspectRatio > targetAspectRatio) {
    // Original is wider - crop width
    cropHeight = originalHeight;
    cropWidth = cropHeight * targetAspectRatio;
  } else {
    // Original is taller or same - crop height
    cropWidth = originalWidth;
    cropHeight = cropWidth / targetAspectRatio;
  }
  
  // Calculate crop position (center)
  const cropX = (originalWidth - cropWidth) / 2;
  const cropY = (originalHeight - cropHeight) / 2;
  
  return {
    x: cropX,
    y: cropY,
    width: cropWidth,
    height: cropHeight
  };
}

// Function to crop image using canvas
function cropImageFromCenter(imageElement, targetAspectRatio, outputWidth = null) {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  
  const originalWidth = imageElement.naturalWidth;
  const originalHeight = imageElement.naturalHeight;
  
  // Calculate crop dimensions
  const crop = calculateCenterCrop(originalWidth, originalHeight, targetAspectRatio);
  
  // Set output dimensions
  const finalWidth = outputWidth || crop.width;
  const finalHeight = finalWidth / targetAspectRatio;
  
  canvas.width = finalWidth;
  canvas.height = finalHeight;
  
  // Draw cropped image
  ctx.drawImage(
    imageElement,
    crop.x, crop.y, crop.width, crop.height,  // Source rectangle
    0, 0, finalWidth, finalHeight             // Destination rectangle
  );
  
  return canvas;
}

// Function to crop from file input
async function cropImageFile(file, targetAspectRatio, outputWidth = 800) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    
    img.onload = () => {
      try {
        const canvas = cropImageFromCenter(img, targetAspectRatio, outputWidth);
        
        // Convert to blob
        canvas.toBlob((blob) => {
          resolve({
            canvas: canvas,
            blob: blob,
            dataUrl: canvas.toDataURL('image/jpeg', 0.9)
          });
        }, 'image/jpeg', 0.9);
      } catch (error) {
        reject(error);
      }
    };
    
    img.onerror = () => reject(new Error('Failed to load image'));
    img.src = URL.createObjectURL(file);
  });
}

// Example usage:
// 1. If you have image dimensions and want to calculate crop area only
const originalWidth = 1920;
const originalHeight = 1080;
const targetAspectRatio = 4/3; // or 1.333...

const cropArea = calculateCenterCrop(originalWidth, originalHeight, targetAspectRatio);
console.log('Crop area:', cropArea);
// Output: { x: 240, y: 0, width: 1440, height: 1080 }

// 2. If you have an image element and want to crop it
const imageElement = document.getElementById('myImage');
const croppedCanvas = cropImageFromCenter(imageElement, 16/9, 1200);
document.body.appendChild(croppedCanvas);

// 3. If you have a file input and want to crop uploaded image
const fileInput = document.getElementById('fileInput');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (file) {
    try {
      const result = await cropImageFile(file, 1/1, 500); // Square crop, 500px width
      document.body.appendChild(result.canvas);
      
      // You can also use result.blob for uploading or result.dataUrl for display
    } catch (error) {
      console.error('Crop failed:', error);
    }
  }
});

// Common aspect ratios
const ASPECT_RATIOS = {
  SQUARE: 1/1,
  LANDSCAPE_4_3: 4/3,
  LANDSCAPE_16_9: 16/9,
  PORTRAIT_3_4: 3/4,
  PORTRAIT_9_16: 9/16,
  GOLDEN_RATIO: 1.618
};
