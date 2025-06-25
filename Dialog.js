(function(document, $) {
    "use strict";

    var IMAGE_FIELD_SELECTOR = '.cq-FileUpload-picker'; // Default pathfield selector
    var CUSTOM_IMAGE_FIELD_SELECTOR = '[data-cq-msm-lockable="fileReference"]'; // Custom selector for file reference field
    
    /**
     * Get image dimensions from image path
     */
    function getImageDimensions(imagePath, callback) {
        if (!imagePath) {
            callback(null, null);
            return;
        }
        
        // Create a temporary image element to get dimensions
        var tempImage = new Image();
        
        tempImage.onload = function() {
            var dimensions = {
                width: this.naturalWidth,
                height: this.naturalHeight,
                aspectRatio: (this.naturalWidth / this.naturalHeight).toFixed(2)
            };
            callback(null, dimensions);
        };
        
        tempImage.onerror = function() {
            callback('Failed to load image', null);
        };
        
        // Load the image - add timestamp to prevent caching issues
        tempImage.src = imagePath + '?t=' + Date.now();
    }
    
    /**
     * Get image information via AEM API
     */
    function getImageInfoViaAPI(imagePath, callback) {
        if (!imagePath || !imagePath.startsWith('/content/dam/')) {
            callback('Invalid image path', null);
            return;
        }
        
        // Use AEM's asset API to get metadata
        var apiUrl = imagePath + '.json';
        
        $.ajax({
            url: apiUrl,
            type: 'GET',
            dataType: 'json',
            success: function(data) {
                var metadata = data['jcr:content'] && data['jcr:content'].metadata;
                if (metadata) {
                    var dimensions = {
                        width: metadata['tiff:ImageWidth'] || metadata.width,
                        height: metadata['tiff:ImageLength'] || metadata.height,
                        aspectRatio: metadata['tiff:ImageWidth'] && metadata['tiff:ImageLength'] ? 
                                   (metadata['tiff:ImageWidth'] / metadata['tiff:ImageLength']).toFixed(2) : null,
                        format: metadata['dc:format'] || metadata.format,
                        size: metadata['dam:size']
                    };
                    callback(null, dimensions);
                } else {
                    callback('No metadata found', null);
                }
            },
            error: function(xhr, status, error) {
                callback('API request failed: ' + error, null);
            }
        });
    }
    
    /**
     * Display image information in dialog
     */
    function displayImageInfo(dimensions, targetElement) {
        if (!dimensions || !targetElement) return;
        
        var infoHtml = '<div class="image-info-display" style="margin-top: 10px; padding: 10px; background: #f5f5f5; border-radius: 4px;">' +
                      '<strong>Image Information:</strong><br>' +
                      'Width: ' + (dimensions.width || 'Unknown') + 'px<br>' +
                      'Height: ' + (dimensions.height || 'Unknown') + 'px<br>' +
                      'Aspect Ratio: ' + (dimensions.aspectRatio || 'Unknown') + '<br>';
        
        if (dimensions.format) {
            infoHtml += 'Format: ' + dimensions.format + '<br>';
        }
        
        if (dimensions.size) {
            infoHtml += 'File Size: ' + formatFileSize(dimensions.size) + '<br>';
        }
        
        infoHtml += '</div>';
        
        // Remove existing info display
        targetElement.find('.image-info-display').remove();
        
        // Add new info display
        targetElement.append(infoHtml);
    }
    
    /**
     * Format file size in human readable format
     */
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        var k = 1024;
        var sizes = ['Bytes', 'KB', 'MB', 'GB'];
        var i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    /**
     * Handle image path change
     */
    function handleImagePathChange(imageField) {
        var $imageField = $(imageField);
        var imagePath = $imageField.val();
        
        console.log('Image path changed:', imagePath);
        
        if (imagePath && imagePath.startsWith('/content/dam/')) {
            // Try API method first (more reliable for DAM assets)
            getImageInfoViaAPI(imagePath, function(error, dimensions) {
                if (!error && dimensions) {
                    displayImageInfo(dimensions, $imageField.closest('.coral-Form-field'));
                } else {
                    // Fallback to direct image loading
                    console.log('API method failed, trying direct image loading:', error);
                    getImageDimensions(imagePath, function(error, dimensions) {
                        if (!error && dimensions) {
                            displayImageInfo(dimensions, $imageField.closest('.coral-Form-field'));
                        } else {
                            console.error('Failed to get image dimensions:', error);
                        }
                    });
                }
            });
        } else {
            // Clear image info if no valid path
            $imageField.closest('.coral-Form-field').find('.image-info-display').remove();
        }
    }
    
    /**
     * Initialize image path monitoring
     */
    function initImagePathMonitoring() {
        // Monitor pathfield changes
        $(document).on('change', 'input[name="./fileReference"], input[name="./imagePath"], .cq-FileUpload-picker input', function() {
            handleImagePathChange(this);
        });
        
        // Monitor coral-pathfield changes
        $(document).on('coral-pathfield:change', 'coral-pathfield[name="./fileReference"], coral-pathfield[name="./imagePath"]', function(e) {
            var pathfield = e.target;
            var imagePath = pathfield.value;
            
            if (imagePath && imagePath.startsWith('/content/dam/')) {
                getImageInfoViaAPI(imagePath, function(error, dimensions) {
                    if (!error && dimensions) {
                        displayImageInfo(dimensions, $(pathfield).closest('.coral-Form-field'));
                    }
                });
            }
        });
        
        // Monitor file upload completion
        $(document).on('assetselected.cq.damadmin', function(e) {
            var selectedAssets = e.selections;
            if (selectedAssets && selectedAssets.length > 0) {
                var assetPath = selectedAssets[0].path;
                setTimeout(function() {
                    // Find the active pathfield and trigger change
                    var activePathfield = $('coral-pathfield:focus, input:focus').filter('[name="./fileReference"], [name="./imagePath"]');
                    if (activePathfield.length > 0) {
                        handleImagePathChange(activePathfield[0]);
                    }
                }, 500);
            }
        });
    }
    
    /**
     * Get current image path from dialog
     */
    function getCurrentImagePath() {
        var imagePathSelectors = [
            'input[name="./fileReference"]',
            'input[name="./imagePath"]',
            'coral-pathfield[name="./fileReference"]',
            'coral-pathfield[name="./imagePath"]'
        ];
        
        for (var i = 0; i < imagePathSelectors.length; i++) {
            var element = $(imagePathSelectors[i]);
            if (element.length > 0 && element.val()) {
                return element.val();
            }
        }
        
        return null;
    }
    
    /**
     * Utility function to get image dimensions (can be called from other scripts)
     */
    window.AEMImageUtils = {
        getImageDimensions: getImageDimensions,
        getImageInfoViaAPI: getImageInfoViaAPI,
        getCurrentImagePath: getCurrentImagePath,
        formatFileSize: formatFileSize
    };
    
    // Initialize when document is ready
    $(document).ready(function() {
        // Wait for dialog to be fully loaded
        setTimeout(function() {
            initImagePathMonitoring();
            
            // Check for existing image paths on dialog load
            $('input[name="./fileReference"], input[name="./imagePath"], coral-pathfield[name="./fileReference"], coral-pathfield[name="./imagePath"]').each(function() {
                if ($(this).val()) {
                    handleImagePathChange(this);
                }
            });
        }, 1000);
    });
    
    // Also initialize on foundation-contentloaded (for Touch UI dialogs)
    $(document).on('foundation-contentloaded', function() {
        setTimeout(function() {
            initImagePathMonitoring();
        }, 500);
    });

})(document, Granite.$);
