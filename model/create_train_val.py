import os
import random
import shutil
from PIL import Image, ExifTags

# Source directories for each cat’s images
catA_source = "data/images/johnny"
catB_source = "data/images/mogli"
else_source = "data/images/else"

# Destination
train_catA = "data/train/johnny"
val_catA   = "data/val/johnny"
train_catB = "data/train/mogli"
val_catB   = "data/val/mogli"
train_else = "data/train/else"
val_else   = "data/val/else"

os.makedirs(train_catA, exist_ok=True)
os.makedirs(val_catA, exist_ok=True)
os.makedirs(train_catB, exist_ok=True)
os.makedirs(val_catB, exist_ok=True)
os.makedirs(train_else, exist_ok=True)
os.makedirs(val_else, exist_ok=True)

# Set your train/val split ratio
train_ratio = 0.8

# Function to fix orientation of images
def fix_orientation(image_path):
    try:
        with Image.open(image_path) as img:
            # Handle EXIF orientation tag if present
            for orientation in ExifTags.TAGS.keys():
                if ExifTags.TAGS[orientation] == 'Orientation':
                    break
            exif = img._getexif()
            if exif and orientation in exif:
                if exif[orientation] == 3:
                    img = img.rotate(180, expand=True)
                elif exif[orientation] == 6:
                    img = img.rotate(270, expand=True)
                elif exif[orientation] == 8:
                    img = img.rotate(90, expand=True)
            # Ensure all images are portrait
            if img.width > img.height:
                img = img.rotate(90, expand=True)
            img.save(image_path)  # Save the corrected image
    except Exception as e:
        print(f"Error processing {image_path}: {e}")

# Function to split one cat’s images
def split_images(source_dir, train_dir, val_dir, train_ratio):
    files = [f for f in os.listdir(source_dir) if f != '.DS_Store']
    random.shuffle(files)
    train_cutoff = int(len(files) * train_ratio)
    train_files = files[:train_cutoff]
    val_files   = files[train_cutoff:]

    for f in train_files:
        source_path = os.path.join(source_dir, f)
        dest_path = os.path.join(train_dir, f)
        fix_orientation(source_path)  # Fix orientation before moving
        shutil.copy(source_path, dest_path)
    for f in val_files:
        source_path = os.path.join(source_dir, f)
        dest_path = os.path.join(val_dir, f)
        fix_orientation(source_path)  # Fix orientation before moving
        shutil.copy(source_path, dest_path)

# Split catA
print("Splitting images for cat A...")
split_images(catA_source, train_catA, val_catA, train_ratio)

# Split catB
print("Splitting images for cat B...")
split_images(catB_source, train_catB, val_catB, train_ratio)

# Split else
print("Splitting images for else...")
split_images(else_source, train_else, val_else, train_ratio)

print("Data split complete!")

