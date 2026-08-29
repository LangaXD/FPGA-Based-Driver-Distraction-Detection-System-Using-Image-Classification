import time
import numpy as np
import cv2
from tflite_runtime import interpreter as tflite

LABEL_NAMES = [
    "safe_driving", "texting_right", "phone_right", "texting_left", "phone_left",
    "adjusting_radio", "drinking", "reaching_behind", "hair_or_makeup", "talking_to_passenger",
]

interp = tflite.Interpreter(model_path="/home/xilinx/mobilenetv2_crossview_finetuned_int8.tflite")
interp.allocate_tensors()
inp = interp.get_input_details()[0]
out = interp.get_output_details()[0]

cap = cv2.VideoCapture('/dev/video0', cv2.CAP_V4L2)
ret, frame = cap.read()
cap.release()
if not ret:
    print("CAMERA_CAPTURE_FAILED")
    raise SystemExit(1)

img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
img = cv2.resize(img, (224, 224))
x = img.astype(inp['dtype'])[None, ...]

t0 = time.time()
interp.set_tensor(inp['index'], x)
interp.invoke()
y = interp.get_tensor(out['index'])[0]
elapsed = time.time() - t0

idx = int(np.argmax(y))
print(f"FRAME_SHAPE={frame.shape}")
print(f"INFERENCE_TIME_S={elapsed:.2f}")
print(f"PREDICTED_CLASS={LABEL_NAMES[idx]} CONFIDENCE={y[idx]:.3f}")
print("END_TO_END_OK")
