import cv2
import numpy as np
from functools import reduce
import operator
import math

# img = cv2.imread('picture_2_segm.png')
def get_corners(img_ext):

    img = img_ext.astype(dtype = np.uint8)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gray = np.float32(gray)
    dst = cv2.cornerHarris(gray,5,3,0.04)
    ret, dst = cv2.threshold(dst,0.1*dst.max(),255,0)
    dst = np.uint8(dst)
    ret, labels, stats, centroids = cv2.connectedComponentsWithStats(dst)
    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 100, 0.001)
    corners = cv2.cornerSubPix(gray,np.float32(centroids),(5,5),(-1,-1),criteria)

    # первая координата - хз что FIXME
    corners = corners[1:]
    corners = np.round(corners).astype(dtype = np.uint8).tolist()

    # сортируем по часовой стрелке
    center = tuple(map(operator.truediv, reduce(lambda x, y: map(operator.add, x, y), corners), [len(corners)] * 2))
    corners = sorted(corners, key=lambda coord: (-135 + math.degrees(math.atan2(*tuple(map(operator.sub, coord, center))[::-1]))) % 360)

    # img[dst>0.1*dst.max()]=[0,0,255]

    return corners