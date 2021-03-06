package org.wheelmap.android.tango.mode;

import android.util.Log;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Polygon;

import org.rajawali3d.Object3D;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Plane;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.wheelmap.android.app.WheelmapApp;
import org.wheelmap.android.online.R;
import org.wheelmap.android.tango.mode.math.SmallestSurroundingRectangle;
import org.wheelmap.android.tango.mode.operations.CreateObjectsOperation;
import org.wheelmap.android.tango.mode.operations.OperationsModeRenderer;
import org.wheelmap.android.tango.renderer.objects.Polygon3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public abstract class MeasureAreaModeRenderer extends OperationsModeRenderer {

    private static final String TAG = MeasureAreaModeRenderer.class.getSimpleName();
    private List<Object3D> pointObjects = new ArrayList<>();

    @Override
    public void onClickedAt(final float[] transform) {

        if (isOperationRunning() || isReady()) {
            return;
        }

        Matrix4 matrix4 = new Matrix4(transform);
        final Vector3 position = matrix4.getTranslation();
        if (pointObjects.size() == 3) {
            projectObjectToPlane(position, new Plane(
                    pointObjects.get(0).getPosition(),
                    pointObjects.get(1).getPosition(),
                    pointObjects.get(2).getPosition()
            ));
        }

        // check if new point has a valid position and does not create a irregular polygon
        List<Vector3> polygonPoints = new ArrayList<>(4);
        for (int i = 0; i < pointObjects.size(); i++) {
            polygonPoints.add(pointObjects.get(i).getPosition());
        }
        polygonPoints.add(position);
        if (isPolygonIrregular(polygonPoints)) {
            Toast.makeText(WheelmapApp.get(), R.string.tango_measurement_area_invalid_position, Toast.LENGTH_SHORT).show();
            return;
        }

        addOperation(new CreateObjectsOperation() {
            @Override
            public void execute(Manipulator m) {

                if (isReady()) {
                    return;
                }

                Object3D createdPoint = getObjectFactory().createMeasurePoint();
                createdPoint.setPosition(position);

                pointObjects.add(createdPoint);
                m.addObject(createdPoint);

                int size = pointObjects.size();


                if (size > 1) {

                    String text = String.format(Locale.getDefault(), "%.2fm", getLastDistance());
                    getObjectFactory().measureLineBetween(m, pointObjects.get(size - 1).getPosition(), pointObjects.get(size - 2).getPosition(), text);

                }

                if (size == 4) {

                    // close polygon by drawing line between first and last point
                    Vector3 first = pointObjects.get(0).getPosition();
                    Vector3 last = pointObjects.get(size - 1).getPosition();

                    String text = String.format(Locale.getDefault(), "%.2fm", first.distanceTo(last));
                    getObjectFactory().measureLineBetween(m, first, last, text);

                    // finish polygon by filling it
                    Stack<Vector3> areaPoints = new Stack<>();
                    for (int i = 0, pointObjectsSize = pointObjects.size(); i < pointObjectsSize; i++) {
                        Object3D pointObject = pointObjects.get(i);
                        areaPoints.add(pointObject.getPosition());
                    }
                    Polygon3D area = getObjectFactory().createAreaPolygon(areaPoints);
                    m.addObject(area);

                }
            }

            @Override
            public void undo(Manipulator manipulator) {
                super.undo(manipulator);
                pointObjects.removeAll(getCreatedObjects());
            }

        });

    }

    @Override
    public boolean isReady() {
        return pointObjects.size() >= 4;
    }

    private double getLastDistance() {
        if (pointObjects.size() < 2) {
            return 0;
        }
        int lastItemPosition = pointObjects.size() - 1;
        Object3D lastItem = pointObjects.get(lastItemPosition);
        int secondLastItemPosition = pointObjects.size() - 2;
        Object3D secondLastItem = pointObjects.get(secondLastItemPosition);
        return lastItem.getPosition().distanceTo(secondLastItem.getPosition());
    }

    private void projectObjectToPlane(Vector3 position, Plane plane) {
        double distance = plane.getDistanceTo(position);
        Vector3 normal = plane.getNormal().clone();
        normal.normalize();
        normal.multiply(-distance);
        position.add(normal);
    }

    private Object3D calculateLastPointOfPolygon() {
        if (pointObjects.size() != 3) {
            throw new IllegalStateException();
        }

        Vector3 a = pointObjects.get(0).getPosition();
        Vector3 b = pointObjects.get(1).getPosition();
        Vector3 c = pointObjects.get(2).getPosition();

        Vector3 ba = b.clone().subtract(a);

        Vector3 position = c.clone().subtract(ba);
        Object3D point4 = getObjectFactory().createMeasurePoint();
        point4.setPosition(position);
        return point4;
    }

    public double getArea() {

        if (pointObjects.size() != 4) {
            return -1;
        }

        List<Vector2> points = projectPolygonTo2d();
        double area = areaOfPolygon(points);
        Log.d(TAG, "Area: " + area);
        return area;
    }

    public double getWidth() {
        if (pointObjects.size() != 4) {
            return -1;
        }

        List<Vector2> vector2s = projectPolygonTo2d();
        Polygon polygon = SmallestSurroundingRectangle.get(vector2s);
        return polygon.getCoordinates()[0].distance(polygon.getCoordinates()[1]);
    }

    public double getLength() {
        if (pointObjects.size() != 4) {
            return -1;
        }

        List<Vector2> vector2s = projectPolygonTo2d();
        Polygon polygon = SmallestSurroundingRectangle.get(vector2s);
        return polygon.getCoordinates()[1].distance(polygon.getCoordinates()[2]);
    }

    private boolean isPolygonIrregular(List<Vector3> points) {
        int size = points.size();
        if (size <= 3) {
            return false;
        }

        double polygonAngle = 0;
        for (int i = 0; i < size; i++) {

            Vector3 one = points.get(i % size);
            Vector3 two = points.get((i + 1) % size);
            Vector3 three = points.get((i + 2) % size);

            Vector3 directionsVector1 = one.clone().subtract(two);
            Vector3 directionsVector2 = two.clone().subtract(three);

            double angle = directionsVector1.angle(directionsVector2);
            polygonAngle += angle;
            Log.d(TAG, "Angle: " + angle);
            if (Math.abs(angle) >= 180) {
                return true;
            }
        }

        Log.d(TAG, "polygonAngle: " + polygonAngle);
        // add some calculation tolerance
        return polygonAngle < 355 || polygonAngle > 365;
    }

    private double areaOfPolygon(List<Vector2> polyPoints) {
        int i, j, n = polyPoints.size();
        double area = 0;

        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            area += polyPoints.get(i).getX() * polyPoints.get(j).getY();
            area -= polyPoints.get(j).getX() * polyPoints.get(i).getY();
        }
        area /= 2.0;
        return Math.abs(area);
    }

    private List<Vector2> projectPolygonTo2d() {
        List<Vector3> polygon3dPoints = new ArrayList<>(4);
        for (int i = 0; i < pointObjects.size(); i++) {
            polygon3dPoints.add(pointObjects.get(i).getPosition());
        }
        return projectPolygonTo2d(polygon3dPoints);
    }

    private List<Vector2> projectPolygonTo2d(List<Vector3> list) {

        // create projection matrix
        Plane plane = new Plane(list.get(0), list.get(1), list.get(2));
        Vector3 u = list.get(0).clone().subtract(list.get(1));
        Vector3 v = plane.getNormal().clone().cross(u);
        Vector3 w = plane.getNormal().clone();
        u.normalize();
        v.normalize();
        w.normalize();

        Matrix4 matrix4 = new Matrix4();
        matrix4.setAll(u, v, w, new Vector3(0, 0, 0));
        matrix4.inverse();

        List<Vector2> vector2s = new ArrayList<>(list.size());
        for (Vector3 vector3 : list) {
            Vector3 projectedVector = matrix4.projectAndCreateVector(vector3);
            vector2s.add(new Vector2(projectedVector.x, projectedVector.y));
        }
        return vector2s;
    }

    @Override
    public void clear() {
        super.clear();
        pointObjects.clear();
    }

}
