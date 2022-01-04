import cv2

# A small script to get a list of available cameras
# TODO: integrate with Java project

cams_test = 10
for i in range(-cams_test, cams_test):
    cap = cv2.VideoCapture(i, cv2.CAP_DSHOW)
    test, frame = cap.read()
    if test:
        print(str(i))