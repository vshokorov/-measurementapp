package com.google.ar.sceneform.samples.hellosceneform;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import image_transformer.BasicTransformer;


public class HelloSceneformActivity extends AppCompatActivity implements Node.OnTapListener, Scene.OnUpdateListener {
    private static final String TAG = HelloSceneformActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    ArrayList<Float> arrayList1 = new ArrayList<>();
    ArrayList<Float> arrayList2 = new ArrayList<>();
    ArrayList<Anchor> anchorsList = new ArrayList<>();
    private ArFragment arFragment;
    private TextView txtDistance;
    int numMeasure = 0;
    Button btnClear;
    PrintStream pPRINT = null;
    final boolean debug_init = false;
    final int crop_width = 70, crop_height = 150;
    ModelRenderable cubeRenderable, heightRenderable;
    float phone_width, phone_height;

    @SuppressLint("SetTextI18n")
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        if (debug_init) {
            Toast.makeText(getApplicationContext(), "Start", Toast.LENGTH_SHORT).show();
        }

        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point outSize = new android.graphics.Point();
        display.getSize(outSize);
        phone_width = outSize.x;
        phone_height = outSize.y;

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        txtDistance = findViewById(R.id.txtDistance);


        btnClear = findViewById(R.id.clear);
        btnClear.setOnClickListener(v -> {
            onClear();
        });

        MaterialFactory.makeTransparentWithColor(this, new Color(0F, 0F, 244F))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.01f, 0.01f, 0.01f);
                            cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });

        MaterialFactory.makeTransparentWithColor(this, new Color(0F, 0F, 244F))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.007f, 0.1f, 0.007f);
                            heightRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            heightRenderable.setShadowCaster(false);
                            heightRenderable.setShadowReceiver(false);
                        });


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                    onClear();

                    if (!Python.isStarted()) {
                        Python.start(new AndroidPlatform(getApplicationContext()));
                    }
                    Python py = Python.getInstance();

                    try {
                        Bitmap bitmap = getBitmapFromView();
                        bitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - crop_width) / 2, (bitmap.getHeight() - crop_height) / 2, crop_width, crop_height);

                        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
                        bitmap.copyPixelsToBuffer(buffer);

                        if (debug_init) {
                            saveBmp2Gallery(bitmap, "croped_" + numMeasure);
                        }

                        PyObject BA = py.getModule("image_transformer").get("BasicTransformer");
                        PyObject ba_po = BA.call(bitmap.getWidth(), bitmap.getHeight());
                        BasicTransformer ba = ba_po.toJava(BasicTransformer.class);

                        ba.set_debug_init(debug_init);

                        int[] mas = ba.get_corners(buffer.array());


                        if (mas.length == 8) {

                            if (debug_init) {
                                int[] mas2 = ba.show_img(buffer.array());

                                Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();

                                for (int i : mas2) {
                                    writeFile("measurements_ok_" + numMeasure, String.valueOf(i));
                                }
                                writeFile("measurements_not_ok_" + numMeasure, "close");
                                numMeasure += 1;
                            }
                            ArrayList<Point> points = new ArrayList<>();


                            // 1.65 - adjusted parameter, because size of bitmap and size of screen are difference
                            final double resizeAdjPar = 1.65;
                            points.add(new Point((int) (bitmap.getWidth() / 2 - (crop_width / 2 - mas[1]) * resizeAdjPar), bitmap.getHeight() / 2 - crop_height / 2 + mas[0]));
                            points.add(new Point((int) (bitmap.getWidth() / 2 - (crop_width / 2 - mas[3]) * resizeAdjPar), bitmap.getHeight() / 2 - crop_height / 2 + mas[2]));
                            points.add(new Point((int) (bitmap.getWidth() / 2 - (crop_width / 2 - mas[5]) * resizeAdjPar), bitmap.getHeight() / 2 - crop_height / 2 + mas[4]));
                            points.add(new Point((int) (bitmap.getWidth() / 2 - (crop_width / 2 - mas[7]) * resizeAdjPar), bitmap.getHeight() / 2 - crop_height / 2 + mas[6]));

                            showCornerAnchor(points, bitmap.getWidth(), bitmap.getHeight());


                        } else {
                            Toast.makeText(this, "repeat", Toast.LENGTH_SHORT).show();

                            if (debug_init) {
                                int[] mas2 = ba.show_img(buffer.array());

                                for (int i : mas2) {
                                    writeFile("measurements_not_ok_" + numMeasure, String.valueOf(i));
                                }
                                writeFile("measurements_not_ok_" + numMeasure, "close");
                                numMeasure += 1;
                            }

                        }

                    } catch (Exception e) {
                        if (debug_init) {
                            Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        e.printStackTrace();
                    }

                });

    }

    public void onResume() {
        super.onResume();

    }

    void writeFile(String name, String msg) {

        if (msg.equals("close")) {
            pPRINT.close();
            pPRINT = null;
        } else {

            try {
                if (pPRINT == null) {
                    String fileName = null;
                    FileOutputStream outStream = null;
                    //System album catalog
                    String filePath = Environment.getExternalStorageDirectory()
                            + File.separator + Environment.DIRECTORY_DCIM
                            + File.separator + "Out_stream" + File.separator;

                    // Declare file objects
                    File file = null;
                    // Declare output stream

                    // If there is a Target file, get the file object directly, otherwise create a file with filename as the name
                    file = new File(filePath, name + ".txt");

                    // Get file relative path
                    fileName = file.toString();
                    // Get the output stream, if there is content in the file, append the content
                    outStream = new FileOutputStream(fileName);
                    if (null != outStream) {
                        pPRINT = new PrintStream(outStream);
                        pPRINT.println(msg);
                    }
                } else {
                    pPRINT.printf(msg + "\n");
                }
            } catch (Exception e) {
                if (debug_init) {
                    Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                e.printStackTrace();
            }
        }
    }


    private void showCornerAnchor(ArrayList<Point> points, int pic_width, int pic_height) {
        AnchorNode anchorNode = new AnchorNode();
        Anchor anchor = null;
        List<HitResult> hitResults = null;
        ArrayList<ArrayList<Float>> vectors = new ArrayList<>();
        ArrayList<Float> vector = null;

        float height, width;

        if (anchorsList.size() > 0) {
            for (Anchor an : anchorsList) {
                an.detach();
                if (debug_init) {
                    Toast.makeText(getApplicationContext(), "detached anchor" + an, Toast.LENGTH_SHORT).show();
                }
            }
        }

        for (Point point : points) {
            try {
                Frame frame = arFragment.getArSceneView().getArFrame();
                hitResults = frame.hitTest((float) point.x * phone_width / pic_width, (float) point.y * phone_height / pic_height + 100);

                // experience shows that the last anchor is more accurate
                HitResult hitResult1 = hitResults.get(hitResults.size() - 1);
                if (debug_init) {
                    Toast.makeText(getApplicationContext(), "hitResults.size(): " + hitResults.size(), Toast.LENGTH_SHORT).show();
                }

                anchor = hitResult1.createAnchor();
                anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                anchorsList.add(anchor);
                Pose pose = anchor.getPose();

                vector = new ArrayList<>();
                vector.add(pose.tx());
                vector.add(pose.ty());
                vector.add(pose.tz());
                vectors.add(vector);

                TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                transformableNode.setParent(anchorNode);
                transformableNode.setRenderable(cubeRenderable);
                transformableNode.select();

            } catch (Exception e) {
                if (debug_init) {
                    Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                e.printStackTrace();

            }
        }

        // 1.09 - adjusted parameter
        final double zoomAdjPar = 1.09;
        if (debug_init) {
            Toast.makeText(getApplicationContext(), String.valueOf(getDistanceMeters(vectors.get(0), vectors.get(3)) * zoomAdjPar), Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), String.valueOf(getDistanceMeters(vectors.get(0), vectors.get(1))), Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), String.valueOf(getDistanceMeters(vectors.get(2), vectors.get(1)) * zoomAdjPar), Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), String.valueOf(getDistanceMeters(vectors.get(2), vectors.get(3))), Toast.LENGTH_SHORT).show();
        }

        width = (getDistanceMeters(vectors.get(0), vectors.get(3)) + getDistanceMeters(vectors.get(1), vectors.get(2))) / 2;
        height = (getDistanceMeters(vectors.get(0), vectors.get(1)) + getDistanceMeters(vectors.get(2), vectors.get(3))) / 2;

        txtDistance.setText("Height: " + Math.max(width, height) * zoomAdjPar + "\nWidth: " + Math.min(width, height));

    }

    private Bitmap getBitmapFromView() {
        Bitmap bitmap = null;
        try {
            Image image = arFragment.getArSceneView().getArFrame().acquireCameraImage();
            byte[] bytes = UtilsBitmap.imageToByte(image);
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            bitmap = UtilsBitmap.rotateBitmap(bitmap, 90);
        } catch (NotYetAvailableException e) {
            e.printStackTrace();
            if (debug_init) {
                Toast.makeText(getApplicationContext(), "getBitmapFromView err: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        return bitmap;
    }


    private void onClear() {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }
        arrayList1.clear();
        arrayList2.clear();
        txtDistance.setText("");
    }

    private float getDistanceMeters(ArrayList<Float> arayList1, ArrayList<Float> arrayList2) {

        float distanceX = arayList1.get(0) - arrayList2.get(0);
        float distanceY = arayList1.get(1) - arrayList2.get(1);
        float distanceZ = arayList1.get(2) - arrayList2.get(2);
        return (float) Math.sqrt(distanceX * distanceX +
                distanceY * distanceY +
                distanceZ * distanceZ);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();
        Box box = (Box) node.getRenderable().getCollisionShape();
        assert box != null;
        Vector3 renderableSize = box.getSize();
        Vector3 transformableNodeScale = node.getWorldScale();
        Vector3 finalSize =
                new Vector3(
                        renderableSize.x * transformableNodeScale.x,
                        renderableSize.y * transformableNodeScale.y,
                        renderableSize.z * transformableNodeScale.z);
        txtDistance.setText("Height: " + finalSize.y);
        Log.e("FinalSize: ", finalSize.x + " " + finalSize.y + " " + finalSize.z);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
//        Collection<Anchor> updatedAnchors = frame.getUpdatedAnchors();
//        for (Anchor anchor : updatedAnchors) {
//            Handle updated anchors...
//        }
    }

    public class Point {

        int x;
        int y;

        Point(int _x, int _y) {
            x = _x;
            y = _y;
        }
    }

    private void saveBmp2Gallery(Bitmap bmp, String picName) {

        String fileName = null;
        //System album catalog
        String galleryPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator;


        // Declare file objects
        File file = null;
        // Declare output stream
        FileOutputStream outStream = null;

        try {
            // If there is a Target file, get the file object directly, otherwise create a file with filename as the name
            file = new File(galleryPath, picName + ".jpg");

            // Get file relative path
            fileName = file.toString();
            // Get the output stream, if there is content in the file, append the content
            outStream = new FileOutputStream(fileName);
            if (null != outStream) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            }

        } catch (Exception e) {
            if (debug_init) {
                Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                if (debug_init) {
                    Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                e.printStackTrace();
            }
        }
        MediaStore.Images.Media.insertImage(getContentResolver(), bmp, fileName, null);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        sendBroadcast(intent);

        Toast.makeText(this, "Finish saving！", Toast.LENGTH_SHORT).show();
    }

}
