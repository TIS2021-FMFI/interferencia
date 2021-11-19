import cv2

# A small script to get available cameras
# TODO: integrate with Java project

cams_test = 500
for i in range(-cams_test, cams_test):
    cap = cv2.VideoCapture(i, cv2.CAP_DSHOW)
    test, frame = cap.read()
    if test:
        print("i : "+str(i)+" /// result: "+str(test))