// Simple approach - add this directly to your dialog's clientlib

(function($) {
    "use strict";
    
    // Function to get and display image dimensions
    function showImageDimensions(imagePath, displayElement) {
        if (!imagePath || !imagePath.startsWith('/content/dam/')) {
            $(displayElement).html('');
            return;
        }
        
        // Method 1: Using asset JSON API
        $.get(imagePath + '.json')
            .done(function(data) {
                var metadata = data['jcr:content'] && data['jcr:content'].metadata;
                if (metadata) {
                    var width = metadata['tiff:ImageWidth'] || metadata.width;
                    var height = metadata['tiff:ImageLength'] || metadata.height;
                    var format = metadata['dc:format'] || 'Unknown';
                    
                    var info = '<div style="background: #e8f4f8; padding: 8px; margin-top: 8px; border-radius: 3px;">' +
                              '<strong>Image Info:</strong><br>' +
                              'Size: ' + width + ' × ' + height + ' pixels<br>' +
                              'Format: ' + format + '<br>' +
                              'Aspect Ratio: ' + (width && height ? (width/height).toFixed(2) : 'N/A') +
                              '</div>';
                    
                    $(displayElement).html(info);
                }
            })
            .fail(function() {
                // Method 2: Fallback - load image directly
                var img = new Image();
                img.onload = function() {
                    var info = '<div style="background: #e8f4f8; padding: 8px; margin-top: 8px; border-radius: 3px;">' +
                              '<strong>Image Info:</strong><br>' +
                              'Size: ' + this.naturalWidth + ' × ' + this.naturalHeight + ' pixels<br>' +
                              'Aspect Ratio: ' + (this.naturalWidth/this.naturalHeight).toFixed(2) +
                              '</div>';
                    $(displayElement).html(info);
                };
                img.onerror = function() {
                    $(displayElement).html('<div style="color: red; margin-top: 8px;">Unable to load image info</div>');
                };
                img.src = imagePath;
            });
    }
    
    // Initialize when dialog loads
    $(document).on('foundation-contentloaded', function() {
        // Find image pathfields and add info display
        $('coral-pathfield[name="./fileReference"], coral-pathfield[name="./imagePath"]').each(function() {
            var $pathfield = $(this);
            var $parent = $pathfield.closest('.coral-Form-field');
            
            // Add info display area
            if ($parent.find('.image-info-area').length === 0) {
                $parent.append('<div class="image-info-area"></div>');
            }
            
            // Show info for existing value
            if ($pathfield.val()) {
                showImageDimensions($pathfield.val(), $parent.find('.image-info-area'));
            }
            
            // Monitor changes
            $pathfield.on('change', function() {
                showImageDimensions($(this).val(), $parent.find('.image-info-area'));
            });
        });
    });
    
})(Granite.$);
