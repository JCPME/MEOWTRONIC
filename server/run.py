from flask import Flask, request
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Directory to store uploaded images
UPLOAD_FOLDER = 'uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

@app.route('/upload', methods=['POST'])
def upload_file():
    """Receive a single image file in a multipart/form-data POST with field name 'image'."""
    if 'image' not in request.files:
        return "No 'image' field in form data", 400

    file = request.files['image']
    if file.filename == '':
        return "No selected file", 400

    filename = secure_filename(file.filename)
    save_path = os.path.join(UPLOAD_FOLDER, filename)
    file.save(save_path)

    print(f"Saved file to {save_path}")
    return "File uploaded successfully", 200

if __name__ == '__main__':
    # Run on 0.0.0.0 so it’s visible on local network.
    # Adjust port if needed, e.g. port=8080
    app.run(host='0.0.0.0', port=5000, debug=True)
