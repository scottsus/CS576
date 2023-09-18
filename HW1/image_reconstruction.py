import cv2
import numpy as np
import matplotlib.pyplot as plt
from scipy.interpolate import griddata

image_list = [
    'lake-forest',
    'miamibeach',
    'mountain',
    'rubixcube',
    'skyclouds',
    'stagforest',
    'worldmap'
]

def remove_samples(image, x):
    """
    Set x% of pixels in image to 0 randomly for all channels
    """
    mask = np.random.choice([0, 1], image.shape[:-1], p=[x/100, 1 - x/100])
    mask = mask[:, :, np.newaxis]

    return image * mask

def reconstruct(image):
    """
    Reconstruct image by interpolating missing values for each channel
    """
    image_filled = image.astype('float64')
    coords = np.array(np.nonzero(image_filled)).T

    grid_x, grid_y = np.mgrid[0:image.shape[0], 0:image.shape[1]]
    
    for ch in range(3):
        valid_coords = coords[np.any(image_filled[coords[:,0], coords[:,1], :] != 0, axis=1)]
        x = valid_coords[:, 1]
        y = valid_coords[:, 0]
        z = image_filled[y, x, ch]

        z_new = griddata((y, x), z, (grid_x, grid_y), method='linear')
        mask_missing = np.all(image_filled == 0, axis=2)
        image_filled[mask_missing, ch] = z_new[mask_missing]

    image_filled = np.nan_to_num(image_filled).astype('uint8')
    return image_filled

def calc_error(original, reconstructed):
    """
    Calculate sum of squared differences
    """
    total_pixels = original.shape[0] * original.shape[1]
    mse = np.sum((original - reconstructed) ** 2) / total_pixels
    return mse

def reconstruct_and_analyze_image(image_name, percentages):
    image = cv2.imread(f'1920x1080_data_samples/{image_name}.jpg')

    downscaled_factor = 1/8
    width = int(image.shape[1] * downscaled_factor)
    height = int(image.shape[0] * downscaled_factor)
    dim = (width, height)
    image = cv2.resize(image, dim, interpolation=cv2.INTER_AREA)

    errors = []
    for percent in percentages:
        corrupted_image = remove_samples(image, percent)
        reconstructed_image = reconstruct(corrupted_image)
        error = calc_error(image, reconstructed_image)
        errors.append(error * 100)
    
    return (errors, image_name)

if __name__ == '__main__':
    percentages = [10, 20, 30, 40, 50]
    for image in image_list:
        (errors, image_name) = reconstruct_and_analyze_image(image, percentages)
        plt.plot(percentages, errors, label=image)
    
    plt.title('Reconstruction Error vs Missing Samples')
    plt.xlabel('Percentage of Missing Samples')
    plt.ylabel('Reconstruction Error')
    plt.legend()
    plt.savefig(f'graphs.png')
    plt.show()
