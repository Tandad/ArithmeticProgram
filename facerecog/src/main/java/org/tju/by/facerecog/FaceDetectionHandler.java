package org.tju.by.facerecog;

import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvDecodeImage;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@WebSocket
public class FaceDetectionHandler extends WebSocketHandler {

    private static final String CASCADE_FILE = "cascade/haarcascade_frontalface_alt.xml";

    private Session mSession;
    private static ArrayList<FaceDetectionHandler> sessions = new ArrayList<FaceDetectionHandler>();

    private CascadeClassifier face_cascade = new CascadeClassifier(CASCADE_FILE);

    public static ArrayList<FaceDetectionHandler> getAllSessions() {
        return sessions;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.register(FaceDetectionHandler.class);
        factory.getPolicy().setMaxBinaryMessageSize(1024 * 512);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        sessions.remove(this);
        System.out.println(
                "Close: statusCode = " + statusCode + ", reason = " + reason + ", sessions = " + sessions.size());
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        mSession = session;
        sessions.add(this);

        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("Message: " + message);
    }

    @OnWebSocketMessage
    public void onBinaryMessage(byte data[], int offset, int length) {
        System.out.println("Binary Message len:" + length);
        if (length > 10000) {
            try {
                byte[] sdata = process(data);
                ByteBuffer byteBuffer = ByteBuffer.wrap(sdata);
                mSession.getRemote().sendBytes(byteBuffer);
                byteBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] process(byte data[]) {
        IplImage originalImage = cvDecodeImage(opencv_core.cvMat(1, data.length, CV_8UC1, new BytePointer(data)));

        Mat videoMat = new Mat(originalImage);
        Mat videoMatGray = new Mat();
        // Convert the current frame to grayscale:
        cvtColor(videoMat, videoMatGray, COLOR_BGRA2GRAY);
        equalizeHist(videoMatGray, videoMatGray);

        // Point p = new Point();
        RectVector faces = new RectVector();
        face_cascade.detectMultiScale(videoMatGray, faces);
        for (int i = 0; i < faces.size(); i++) {
            Rect face_i = faces.get(i);

            //Mat face = new Mat(videoMatGray, face_i);
            // If fisher face recognizer is used, the face need to be
            // resized.
            // resize(face, face_resized, new Size(im_width, im_height),
            // 1.0, 1.0, INTER_CUBIC);

            // Now perform the prediction, see how easy that is:
            // int prediction = lbphFaceRecognizer.predict(face);

            // And finally write all we've found out to the original image!
            // First of all draw a green rectangle around the detected face:
            rectangle(videoMat, face_i, new Scalar(0, 255, 0, 1));

            System.out.println("face pos: x:" + face_i.x() + " y:" + face_i.y());

            // Create the text we will annotate the box with:
            //String box_text = "Prediction = " + prediction;
            // Calculate the position for annotated text (make sure we don't
            // put illegal values in there):
            //int pos_x = Math.max(face_i.tl().x() - 10, 0);
            //int pos_y = Math.max(face_i.tl().y() - 10, 0);
            // And now put it into the image:
            //putText(videoMat, box_text, new Point(pos_x, pos_y), FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 255, 0, 2.0));
        }

        // JavaCVUtil.imShow(videoMat, "test");

        return getMatByteBuffer(videoMat);
    }

    private byte[] getMatByteBuffer(Mat m) {
        byte[] result = null;
        try {
            ToMat convert = new ToMat();
            Frame frame = convert.convert(m);
            Java2DFrameConverter java2dFrameConverter = new Java2DFrameConverter();
            BufferedImage bufferedImage = java2dFrameConverter.convert(frame);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", out);
            result = out.toByteArray();
            out.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return result;
    }
}