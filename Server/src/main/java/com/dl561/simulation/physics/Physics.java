package com.dl561.simulation.physics;

import com.dl561.simulation.Simulation;
import com.dl561.simulation.Tick;
import com.dl561.simulation.vehicle.MinMax;
import com.dl561.simulation.vehicle.Vehicle;
import com.dl561.simulation.vehicle.Vertex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Physics {

    @Value("${physics.message.debug}")
    private boolean showDebug;
    @Value("${physics.message.summary}")
    private boolean showSummary;
    private static final double precision = 0.1d;
    private static final double GRAVITY = 9.81d;
    final double CA_R = -5.20;   /* cornering stiffness */
    final double CA_F = -5.0;    /* cornering stiffness */
    final double MAX_GRIP = 2.0;

    /**
     * Runs the simulation one tick
     *
     * @param simulation The simulation to run
     */
    public void simulate(Simulation simulation) {
        if (showDebug) {
            System.out.println("Calculating new positions");
        }
        applyVehicleForcesToVehicles(simulation.getVehicles());
        if (checkAllCollisions(simulation)) {
            //There has been a collision
            CollisionData collisionData = findCollision(simulation);
            resolveThis(collisionData);
        }
        /**
         int collisionCount = 0;
         while (checkAllCollisions(simulation) && collisionCount < 100) {
         CollisionData collisionData = findCollision(simulation);
         //TODO: this still doesn't work
         while (collisionData.getCollisionType().equals(CollisionType.PENETRATING)) {
         shiftCollidingObjects(collisionData);
         collisionData = findCollision(simulation);
         }
         if (collisionData.getCollisionType().equals(CollisionType.NONE)) {
         break;
         }
         resolveCollision(collisionData);
         collisionCount++;
         }
         **/
    }

    private void resolveThis(CollisionData collisionData) {
        if (collisionData == null) {
            return;
        }
        double forceMultiplier = 1000;
        Collidable a = collisionData.getA();
        Collidable b = collisionData.getB();

        //TODO: try just oppositing the velocities

        double aXCentre = a.getX() + (a.getWidth() / 2);
        double aYCentre = a.getY() + (a.getLength() / 2);

        double bXCentre = b.getX() + (b.getWidth() / 2);
        double bYCentre = b.getY() + (b.getLength() / 2);

        double dx = Math.abs(aXCentre - bXCentre);
        double dy = Math.abs(aYCentre - bYCentre);
        double distanceBetween = Math.sqrt(dx * dx + dy * dy);

        double bounceForce = forceMultiplier / distanceBetween;
//        System.out.println("bounceForce: " + bounceForce);

        Vector2D force = getUnitVector(new Vector2D(dx, dy)).multiply(bounceForce);

        if (a.isMovable()) {
            a.applyCollision(force);
        }
        if (b.isMovable()) {
            b.applyCollision(force.multiply(-1));
        }
    }

    /**
     * Moves the objects that are colliding slightly so they are not penetrating
     *
     * @param collisionData
     */
    private void shiftCollidingObjects(CollisionData collisionData) {
        collisionData.getA().nudge(collisionData.getSideNormal().getNormal());
    }

    /**
     * Calculates the new positions and velocities of colliding objects
     *
     * @param collisionData
     */
    private void resolveCollision(CollisionData collisionData) {
        Collidable a = collisionData.getA();
        Vector2D collidingVertex = collisionData.getCollidingCorner().asVector2D();
        Edge collisionNormal = collisionData.getSideNormal();
        Vector2D aCentreMass = a.getCentreMass();

        Vector2D cmToCornerPerp = getPerpendicularVector(collidingVertex.subtract(aCentreMass));

        Vector2D velocity = collisionData.getVelocityOfCorner();

        double impulseNumerator = -(1 + a.getCoefficientOfResitution()) * velocity.dotProduct(collisionNormal.getNormal());

        double perpDot = cmToCornerPerp.dotProduct(collisionNormal.getNormal());

        double impuleDenominator = (1 / a.getMass()) + (1 / a.getInertia()) * perpDot * perpDot;

        double impulse = impulseNumerator / impuleDenominator;

        Vector2D centreMassVelocityAddition = collisionNormal.getNormal().multiply(impulse * (1 / a.getMass()));

        double angularVelocityAddition = impulse * (1 / a.getInertia()) * perpDot;

        //TODO: this really doesn't seem correct. (It gives NaN straight away)

        collisionData.getA().applyCollision(centreMassVelocityAddition, angularVelocityAddition);
    }

    /**
     * Finds whether there are any collisions between collidable objects
     *
     * @param simulation
     * @return CollisionData    data that is needed to resolve any collison
     */
    public CollisionData findCollision(Simulation simulation) {
        List<Collidable> collidables = simulation.getAllCollidables();
        for (Collidable collidable1 : collidables) {
            if (collidable1.isMovable()) {
                for (Collidable collidable2 : collidables) {
                    if (!collidable1.equals(collidable2)) {
                        if (checkCollisionSingle(collidable1, collidable2)) {
                            return findCollisionData(collidable1, collidable2);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finding the data about the collision for two collidables
     *
     * @param a The first collidable
     * @param b The second collidable
     * @return CollisionData    The data needed to resolve this collision
     */
    public CollisionData findCollisionData(Collidable a, Collidable b) {
        List<Vertex> vertices = a.getRectangleVertices();
        CollisionData collisionData = new CollisionData();
        for (int i = 0; i < 4; i++) {
            Vector2D positionVector = vertices.get(i).asVector2D();

            Vector2D cmToCornerPerpendicular = getPerpendicularVector(positionVector);

            Vector2D velocityOfCorner = a.getVelocity().add(cmToCornerPerpendicular.multiply(a.getAngularVelocity()));

            List<Edge> edgesOfB = b.getEdges();
            for (Edge edge : edgesOfB) {

                double intersection = positionVector.dotProduct(edge.getNormal()) + edge.getLength();

                if (intersection < -1d) {
                    //TODO: Always penetrating
//                    System.out.println("Penetrating");
                    collisionData.setCollisionType(CollisionType.PENETRATING);
                    collisionData.setCollisionStatus(true);
                } else if (intersection < 1d) {
                    double relativeVelocity = edge.getNormal().dotProduct(velocityOfCorner);
                    if (relativeVelocity < 0d) {
//                        System.out.println("Colliding");
                        collisionData.setCollisionType(CollisionType.COLLIDING);
                        collisionData.setCollisionStatus(true);
                    }
                }
                if (collisionData.isCollisionStatus()) {
                    collisionData.setA(a);
                    collisionData.setB(b);
                    collisionData.setSideNormal(edge);
                    collisionData.setCollidingCorner(vertices.get(i));
                    collisionData.setVelocityOfCorner(velocityOfCorner);
                    return collisionData;
                }
            }
        }
        return null;
    }

    /**
     * Checks for any collisions between all objects
     * It only looks at whether a movable object is colliding with a solid object
     *
     * @param simulation
     * @return boolean      Whether there has been a collision
     */
    public boolean checkAllCollisions(Simulation simulation) {
        List<Collidable> collidables = simulation.getAllCollidables();
        for (Collidable collidable1 : collidables) {
            if (collidable1.isMovable()) {
                //collidable1 can move
                for (Collidable collidable2 : collidables) {
                    if (!collidable1.equals(collidable2)) {
                        //collidable2 is not equal to collidable1
                        if (collidable2.isSolid()) {
                            //collidable2 is a solid object
                            if (checkCollisionSingle(collidable1, collidable2)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks for a collision between two collidable objects
     *
     * @param collidable
     * @param other
     * @return boolean      Whether there is a collision of any kind
     */
    public boolean checkCollisionSingle(Collidable collidable, Collidable other) {
        Vector2D normal1 = collidable.getNormalVector1();
        Vector2D normal2 = collidable.getNormalVector2();
        Vector2D normal3 = other.getNormalVector1();
        Vector2D normal4 = other.getNormalVector2();

        MinMax a1 = getMinAndMax(collidable, normal1);
        MinMax b1 = getMinAndMax(collidable, normal1);

        MinMax a2 = getMinAndMax(collidable, normal2);
        MinMax b2 = getMinAndMax(other, normal2);

        MinMax a3 = getMinAndMax(collidable, normal3);
        MinMax b3 = getMinAndMax(other, normal3);

        MinMax a4 = getMinAndMax(collidable, normal4);
        MinMax b4 = getMinAndMax(other, normal4);

        if (a1.getMax() < b1.getMin() || b1.getMax() < a1.getMin()) {
            return false;
        } else if (a2.getMax() < b2.getMin() || b2.getMax() < a2.getMin()) {
            return false;
        } else if (a3.getMax() < b3.getMin() || b3.getMax() < a3.getMin()) {
            return false;
        } else if (a4.getMax() < b4.getMin() || b4.getMax() < a4.getMin()) {
            return false;
        }
        return true;
    }

    /**
     * Acting on the collision between vehicles
     *
     * @param vehicle1
     * @param vehicle2
     */
    public void doCollision(Vehicle vehicle1, Vehicle vehicle2) {
//        System.out.println("Collision Happened");
//        Vector2D shortestSeperation = findShortestSeperationVector(vehicle1, vehicle2);
//        Vector2D normalUnit = getUnitVector(shortestSeperation);
//        Vector2D relativeMomentum = new Vector2D();
//        relativeMomentum.setX(vehicle1.getWRXVelocity() * vehicle1.getMass() - vehicle2.getWRXVelocity() * vehicle2.getMass());
//        relativeMomentum.setY(vehicle1.getWRYVelocity() * vehicle1.getMass() - vehicle2.getWRYVelocity() * vehicle2.getMass());
//        double penetrationSpeed = 1;
    }

    /**
     * Finding the minimum and maximum distance of the dotProduct along the normal
     *
     * @param collidable
     * @param normal
     * @return MinMax   The min and max that is currently there
     */
    private MinMax getMinAndMax(Collidable collidable, Vector2D normal) {
        List<Vertex> vertices = collidable.getRectangleVertices();
        double currentMin = vertices.get(0).asVector2D().dotProduct(normal);
        double currentMax = vertices.get(0).asVector2D().dotProduct(normal);
        for (Vertex vertex : vertices) {
            double projection = vertex.asVector2D().dotProduct(normal);
            if (projection > currentMax) {
                currentMax = projection;
            }
            if (projection < currentMin) {
                currentMin = projection;
            }
        }
        return new MinMax(currentMin, currentMax);
    }

    /**
     * Initial force applied to the wheels from braking, engine, rolling and drag
     *
     * @param vehicle
     * @return Vector2D     The force applied
     */
    private Vector2D calculateInitialWheelForce(Vehicle vehicle) {
        Vector2D velocity = vehicle.getVehicleReferenceVelocity();
        double brakingForce = -vehicle.getMaxBrakingForce() * vehicle.getBrakePedalDepth();
        double engineForce = vehicle.getMaxEngineForce() * vehicle.getAcceleratorPedalDepth();
        double rollingForce = -vehicle.getRollingResistanceConstant() * velocity.getX();
        double dragForce = -vehicle.getDragConstant() * velocity.getX() * Math.abs(velocity.getX());
        Vector2D initialForce = new Vector2D();
        initialForce.setY(0);
        initialForce.setX(engineForce + brakingForce + rollingForce + dragForce);
        return initialForce;
    }

    /**
     * Calculating and applying all forces to each vehicle
     *
     * @param vehicleList
     */
    public void applyVehicleForcesToVehicles(List<Vehicle> vehicleList) {
        for (Vehicle vehicle : vehicleList) {
            applyVehicleForces(vehicle);
        }
    }

    /**
     * Calculating and applying all forces to this vehicle
     *
     * @param vehicle
     */
    private void applyVehicleForces(Vehicle vehicle) {
        Vector2D velocity = vehicle.getVehicleReferenceVelocity();

        velocity.setX(normalise(velocity.getX(), precision));
        velocity.setY(normalise(velocity.getY(), precision));

        double rotationAngle = 0;
        double sideSlip = 0;
        if (velocity.getX() != 0) {
            double yawSpeed = 0.5 * vehicle.getWheelBaseConstant() * vehicle.getAngularVelocity();
            rotationAngle = Math.atan(yawSpeed / velocity.getX());
            sideSlip = Math.atan(velocity.getY() / velocity.getX());
        } else {
            vehicle.setAngularVelocity(0);
        }

        double slipAngleFront = sideSlip + rotationAngle - (vehicle.getSteeringWheelDirection() / 10);
        double slipAngleRear = sideSlip - rotationAngle;

        double frontWheelWeight = calculateWheelWeight(vehicle, true);
        double rearWheelWeight = calculateWheelWeight(vehicle, false);

        Vector2D frontWheelLateralForce = calculateLateralForce(CA_F, slipAngleFront, frontWheelWeight, vehicle, true);
        Vector2D rearWheelLateralForce = calculateLateralForce(CA_R, slipAngleRear, rearWheelWeight, vehicle, false);

        Vector2D tractionForce = calculateTractionForceV2(vehicle);

        Vector2D rollingResistance = calculateRollingResistanceForce(vehicle.getRollingResistanceConstant(), velocity);
        Vector2D dragResistance = calculateDragForce(vehicle.getDragConstant(), velocity);

        Vector2D resistance = new Vector2D();
        resistance.setX(rollingResistance.getX() + dragResistance.getX());
        resistance.setY(rollingResistance.getY() + dragResistance.getY());

        Vector2D totalForce = new Vector2D();
        totalForce.setX(tractionForce.getX() + frontWheelLateralForce.getX() + rearWheelLateralForce.getX() + resistance.getX());
        totalForce.setY(tractionForce.getY() + frontWheelLateralForce.getY() + rearWheelLateralForce.getY() + resistance.getY());


        Vector2D acceleration = new Vector2D();
        acceleration.setX(totalForce.getX() / vehicle.getMass());
        acceleration.setY(totalForce.getY() / vehicle.getMass());

        acceleration.setX(normalise(acceleration.getX(), precision));
        acceleration.setY(normalise(acceleration.getY(), precision));

        Vector2D worldReferenceAcceleration = new Vector2D();
        worldReferenceAcceleration.setX(vehicle.getCos() * acceleration.getY() + vehicle.getSin() * acceleration.getX());
        worldReferenceAcceleration.setY(-vehicle.getSin() * acceleration.getY() + vehicle.getCos() * acceleration.getX());

        Vector2D worldReferenceVelocity = new Vector2D();
        worldReferenceVelocity.setX(vehicle.getWRXVelocity() + (Tick.SECONDS_PER_TICK * worldReferenceAcceleration.getX()));
        worldReferenceVelocity.setY(vehicle.getWRYVelocity() + (Tick.SECONDS_PER_TICK * worldReferenceAcceleration.getY()));

        Vector2D newPosition = new Vector2D();
        newPosition.setX(Tick.SECONDS_PER_TICK * -worldReferenceVelocity.getX() + vehicle.getLocation().getX());
        newPosition.setY(Tick.SECONDS_PER_TICK * worldReferenceVelocity.getY() + vehicle.getLocation().getY());


        vehicle.setWRXVelocity(normalise(worldReferenceVelocity.getX(), precision));
        vehicle.setWRYVelocity(normalise(worldReferenceVelocity.getY(), precision));

        double vehicleTorque = calculateTorque(frontWheelLateralForce, rearWheelLateralForce, vehicle);
        double angularAcceleration = normalise(vehicleTorque / vehicle.getInertia(), 0.02d);
        if (vehicle.getWRXVelocity() == 0 && vehicle.getWRYVelocity() == 0) {
            vehicle.setAngularVelocity(0);
        } else {
            vehicle.setAngularVelocity(normalise(normalise(-0.5, 0.5, vehicle.getAngularVelocity() + (Tick.SECONDS_PER_TICK * angularAcceleration)), 0.022d));
        }

        vehicle.setDirectionOfTravel(vehicle.getDirectionOfTravel() + Tick.SECONDS_PER_TICK * vehicle.getAngularVelocity());
        vehicle.setLocation(newPosition);
        vehicle.setTorque(vehicleTorque);
        vehicle.setVehicleReferenceXAcceleration(acceleration.getX());
        vehicle.setVehicleReferenceYAcceleration(acceleration.getY());
        vehicle.setWorldReferenceXAcceleration(worldReferenceAcceleration.getX());
        vehicle.setWorldReferenceYAcceleration(worldReferenceAcceleration.getY());
        vehicle.setAngularAcceleration(angularAcceleration);
        vehicle.setRpm(calculateRpm(vehicle));
    }

    private int calculateRpm(Vehicle vehicle) {
        double wheelRotationRate = vehicle.getSpeed() / vehicle.getWheelRadius();
        //TODO: check what this gives, doesn't really matter, is already a hack
        int rpm = (int) (wheelRotationRate * vehicle.getGearRatio(vehicle.getGear()) * 60 / (2 * Math.PI));
        return rpm < 1000 ? 1000 : rpm;
    }

    /**
     * Calculating the torque on the vehicle body from the lateral forces
     *
     * @param frontWheelLateralForce
     * @param rearWheelLateralForce
     * @param vehicle
     * @return
     */
    private double calculateTorque(Vector2D frontWheelLateralForce, Vector2D rearWheelLateralForce, Vehicle vehicle) {
        double frontTorque = frontWheelLateralForce.getY() * vehicle.getFrontWheelBase();
        double rearTorque = rearWheelLateralForce.getY() * vehicle.getRearWheelBase();
        return frontTorque - rearTorque;
    }

    private double calculateWheelWeight(Vehicle vehicle, boolean front) {
        double stationaryWheelWeight = 0.5 * vehicle.getWeight();
        //TODO: put wheel weight back in
        return stationaryWheelWeight;
        /**
         double centreOfGravity = vehicle.getCentreOfGravityHeight() / vehicle.getWheelBaseConstant();
         double centreOfGravityWithMassAndAcceleration = centreOfGravity * vehicle.getMass() * vehicle.getVehicleReferenceXAcceleration();
         double wheelWeight = stationaryWheelWeight;
         if (front) {
         wheelWeight -= centreOfGravityWithMassAndAcceleration;
         } else {
         wheelWeight += centreOfGravityWithMassAndAcceleration;
         }
         return wheelWeight;
         **/
    }

    public Vector2D calculateTractionForceV2(Vehicle vehicle) {
        Vector2D tractionForce = new Vector2D();
        tractionForce.setX(vehicle.getAcceleratorPedalDepth() * vehicle.getMaxEngineTorque(vehicle.getRpm()) * vehicle.getGearRatio(vehicle.getGear()) * vehicle.getWheelRadius());
        tractionForce.setX(tractionForce.getX() + (vehicle.getMaxBrakingForce() * vehicle.getBrakePedalDepth()));
        if (tractionForce.getX() < 0) {
            tractionForce.setX(0);
        }
        tractionForce.setY(0);

        if (vehicle.isRearSlip()) {
            tractionForce.setX(tractionForce.getX() * 0.5d);
        }
        return tractionForce;
        //TODO: Add in braking force somewhere
    }

    public Vector2D calculateBrakingForce(Vehicle vehicle) {
        Vector2D brakingForce = new Vector2D();
        brakingForce.setX(vehicle.getBrakePedalDepth() / 100 * vehicle.getMaxBrakingForce());
        brakingForce.setY(0);
        if (brakingForce.getX() >= (vehicle.getMaxBrakingForce() * 80 / 100)) {
            vehicle.setFrontSlip(true);
            vehicle.setRearSlip(true);
        } else {
            vehicle.setFrontSlip(false);
            vehicle.setRearSlip(false);
        }
        return brakingForce;
    }

    public Vector2D calculateTractionForce(Vehicle vehicle) {
        Vector2D tractionForce = new Vector2D();
        tractionForce.setX(calculateXEngineForce(vehicle));
        tractionForce.setY(0);

        if (vehicle.isRearSlip()) {
            //TODO: change this 0.5d based on the surface
            tractionForce.setX(tractionForce.getX() * 0.5d);
        }
        return tractionForce;
    }

    public double calculateXEngineForce(Vehicle vehicle) {
        double force = vehicle.getAcceleratorPedalDepth() * vehicle.getMaxEngineForce() * vehicle.getGearRatio(vehicle.getGear());
        //TODO: Does this only let you brake if you're going forwards?
        if (Math.abs(normalise(vehicle.getVehicleReferenceXAcceleration(), precision)) > 0) {
            force -= vehicle.getBrakePedalDepth() * vehicle.getMaxBrakingForce();
        }
        return force;
    }

    public Vector2D calculateRollingResistanceForce(double rollingResistanceConstant, Vector2D vehicleReferenceVelocity) {
        Vector2D rollingResistance = new Vector2D();
        rollingResistance.setX(-rollingResistanceConstant * vehicleReferenceVelocity.getX());
        rollingResistance.setY(-rollingResistanceConstant * vehicleReferenceVelocity.getY());
        return rollingResistance;
    }

    public Vector2D calculateDragForce(double dragConstant, Vector2D vehicleReferenceVelocity) {
        Vector2D drag = new Vector2D();
        drag.setX(-dragConstant * vehicleReferenceVelocity.getX() * Math.abs(vehicleReferenceVelocity.getX()));
        drag.setY(-dragConstant * vehicleReferenceVelocity.getY() * Math.abs(vehicleReferenceVelocity.getY()));
        return drag;
    }

    private Vector2D calculateLateralForce(double corneringStiffness, double slipAngle, double wheelWeight, Vehicle vehicle, boolean isFront) {
        boolean slip = (isFront) ? vehicle.isFrontSlip() : vehicle.isRearSlip();
        Vector2D lateralForce = new Vector2D();
        lateralForce.setX(0);
        lateralForce.setY(normalise(-MAX_GRIP, MAX_GRIP, corneringStiffness * slipAngle));
        lateralForce.setY(lateralForce.getY() * wheelWeight);

        if (slip) {
            //TODO: change this 0.5d to a variable based on the surface
            lateralForce.setY(lateralForce.getY() * 0.5d);
        }

        if (isFront) {
            lateralForce.setX(Math.sin(vehicle.getSteeringWheelDirection()) * lateralForce.getX());
            lateralForce.setY(Math.cos(vehicle.getSteeringWheelDirection()) * lateralForce.getY());
        }
        return lateralForce;
    }

    public static double normalise(double min, double max, double value) {
        if (value >= min && value <= max) {
            return value;
        } else if (value <= min) {
            return min;
        } else if (value >= max) {
            return max;
        } else {
            System.out.println("Normalisation failed!?!?! for " + min + " < " + value + " < " + max);
            return value;
        }
    }

    public static double normalise(double value, double precision) {
        if (value > 0 && value < precision) {
            return 0;
        } else if (value < 0 && value > -precision) {
            return 0;
        }
        return value;
    }

    public static double normaliseBearing(double bearing) {
        while (bearing < 0) {
            bearing += 2 * Math.PI;
        }
        return bearing % (2 * Math.PI);
    }

    public static Vector2D getUnitVector(Vector2D vector) {
        double mag = vector.getMagnitude();
        return new Vector2D(vector.getX() / mag, vector.getY() / mag);
    }


    public Vector2D getPerpendicularVector(Vector2D vector) {
        return new Vector2D(-vector.getY(), vector.getX());
    }
}
