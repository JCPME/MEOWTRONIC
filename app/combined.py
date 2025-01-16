import cv2
import torch
import torchvision.models as models
import torch.nn as nn
import torchvision.transforms as transforms
from PIL import Image
import numpy as np

# Class labels
CLASS_NAMES = ["Else", "Johnny", "Mogli"]


def load_model(weights_path="cat_classifier.pth"):
    model = models.resnet18()
    num_features = model.fc.in_features
    model.fc = nn.Linear(num_features, len(CLASS_NAMES))
    model.load_state_dict(torch.load(weights_path, map_location="cpu"))
    model.eval()
    return model

# Load YOLOv5 model for cat detection
yolo_model = torch.hub.load('ultralytics/yolov5', 'yolov5s', pretrained=True)
yolo_model.eval()
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
yolo_model.to(device)

# Load your cat classifier model
# Replace load_model() with your actual classifier loading function
# Ensure that the classifier and YOLO models use consistent device placement
cat_classifier = load_model("cat_classifier.pth")  # your function to load classifier
cat_classifier.eval()
cat_classifier.to(device)

# Define transformation for classifier input images (adjust to match training)
import torchvision.transforms as transforms
TRANSFORM = transforms.Compose([
    transforms.Resize((224, 224)),  # ensure this matches classifier's expected input size
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406],
                         [0.229, 0.224, 0.225])
])


# Initialize video capture
cap = cv2.VideoCapture(0)  # use appropriate source

if not cap.isOpened():
    print("Error: Could not open video stream.")
    exit()

while True:
    ret, frame = cap.read()
    if not ret:
        print("Failed to grab frame.")
        break

    # Convert frame to PIL image for YOLO inference
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(img_rgb)

    # Run YOLOv5 detection
    results = yolo_model(pil_img)
    detections = results.xyxy[0]  # bounding boxes for first image

    # Process each detection
    for *box, conf, cls in detections:
        # Get class name from YOLO model
        detected_class = yolo_model.names[int(cls)]
        
        # Proceed only if a cat is detected (you can refine this condition as needed)
        if detected_class == 'cat':
            x1, y1, x2, y2 = map(int, box)
            
            # Ensure coordinates are within frame boundaries
            h, w, _ = frame.shape
            x1 = max(0, x1)
            y1 = max(0, y1)
            x2 = min(w, x2)
            y2 = min(h, y2)

            # Crop the detected cat region from the original frame
            cat_crop = frame[y1:y2, x1:x2]  # OpenCV format (BGR)
            # Convert cropped image to PIL RGB for classifier
            cat_crop_rgb = cv2.cvtColor(cat_crop, cv2.COLOR_BGR2RGB)
            pil_cat_crop = Image.fromarray(cat_crop_rgb)

            # Preprocess crop for classifier
            input_tensor = TRANSFORM(pil_cat_crop).unsqueeze(0).to(device)

            # Classify the cropped image
            with torch.no_grad():
                output = cat_classifier(input_tensor)
                probabilities = torch.softmax(output, dim=1)[0]
                confidence, predicted_idx = torch.max(probabilities, dim=0)
            predicted_label = CLASS_NAMES[predicted_idx.item()]
            label_text = f"{predicted_label} {confidence.item():.2f}"

            # Draw bounding box and label on original frame
            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(frame, label_text, (x1, y1 - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    # Display the annotated frame
    cv2.imshow('Cat Detection and Recognition', frame)

    # Exit on pressing 'q'
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
