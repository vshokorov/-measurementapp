from __future__ import absolute_import, division, print_function

from java import constructor, method, static_proxy, jint
from java import cast, jarray, jbyte, jboolean

import numpy as np
from array import array
import arcady_module as am

class BasicTransformer(static_proxy()):
    @constructor([jint])
    def __init__(self, w, h):
        super(BasicTransformer, self).__init__()
        self.width = w
        print("Python: w:" + str(w) + ', h:' + str(h))
        self.height = h

    @method(jboolean , [jboolean])
    def set_debug_init(self, debug_init):
        self.debug_init = debug_init
        return True

    @method(jarray(jint), [jarray(jbyte)])
    def show_img(self, image_data):
        t = bytes(image_data)
        if (self.debug_init):
            print("Python: bytes ready")
        y = np.frombuffer(t, dtype=np.uint8)
        if (self.debug_init):
            print("Python: y:" + str(-y[:100]))

        y = np.array(list(map(lambda x : x[:3] , (y).reshape((-1, 4)))))
        if (self.debug_init):
            print("Python: y:" + str(y[:3]))


        y = y.reshape((self.height, self.width, 3)) + 128
        return array('l', list(y.reshape((-1))) )

    @method(jarray(jint), [jarray(jbyte)])
    def get_corners(self, image_data):
        if (self.debug_init):
            print("Python: Start!")

        if (self.debug_init):
            print("Python: Start!")

        t = bytes(image_data)
        if (self.debug_init):
            print("Python: bytes ready")
        y = np.frombuffer(t, dtype=np.uint8)
        if (self.debug_init):
            print("Python: y:" + str(-y[:100]))

        y = y.reshape((self.height, self.width, 4))
        y = y[:, :, :3]

        if (self.debug_init):
            print("Python: y:" + str(y[:3]))

        if (self.debug_init):
            print("Python: y.shape:" + str(y.shape))
            print("Python: " + str(y.min()) + '< > ' + str(y.max()))
            print("Python: " + str(type(y)))
            print("Python: " + str(y.shape))

        mas = am.get_corners(y)

        if (self.debug_init):
            print("Python: mas:" + str(mas))
            print("Python: " + str(len(y)))

        if len(mas) == 0:
            mas = [[0]]
        else:
            if (self.debug_init):
                print("Python: mas: " + str(mas) )
            mas_ = []
            for i in mas:
                mas_ += [i[1], i[0]]

            if (self.debug_init):
                print("Python: mas_: " + str(mas_) )

        return array('l', mas_ )