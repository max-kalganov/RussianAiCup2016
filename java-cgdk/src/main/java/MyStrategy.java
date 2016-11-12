import model.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    private static final double WAYPOINT_RADIUS = 100.0D;

    private static final double LOW_HP_FACTOR = 0.25D;
    private final Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);

    private Random random;

    private LaneType lane;
    private Point2D[] waypoints;

    private Wizard self;
    private World world;
    private Game game;
    private Move move;
    //  константа для дистанции , если она не определена
    private final double wrongDistance = -1;
    //последнее изменение позиции 
    private static double changingPosition = 0;
    // предыдущая позиция
    private static Point2D prevPos = new Point2D(0,0);
    
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
    	
        initializeStrategy(self, game);
        initializeTick(self, world, game, move);
        
 
        move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());
        // определяем дистанцию до ближайшей цели
        LivingUnit nearestTarget = getNearestTarget();
        double distance = wrongDistance;
        if (nearestTarget != null) {
            distance = self.getDistanceTo(nearestTarget);
         // отходим при малых хп
        if ((self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) && (distance < 600)) {
            goTo(getPreviousWaypoint());
            return;
        }
        //отходим , если противник ближе 300 (500 - дальность стрельбы)
        if ((distance != wrongDistance)&&(distance <= 300)) {
            goTo(getPreviousWaypoint());
            return;
        }
        //выходим из тупика 
        Point2D curPos = new Point2D(self.getX(),self.getY());
        if (prevPos == curPos){
        	changingPosition++;
        	if (changingPosition == 5) {
                goTo(getPreviousWaypoint());
                return;
            }
        }
        else {
        	changingPosition = 0;
        	prevPos = curPos;
        }
        
            
            if (distance <= self.getCastRange()) {
              // узнаём угол до врага 
            	double angle = self.getAngleTo(nearestTarget);
            	// поворачиваемся 
                move.setTurn(angle);
                
                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    move.setAction(ActionType.MAGIC_MISSILE);
                    move.setCastAngle(angle);
                    move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());
                }

                return;
            }
        }

        // Åñëè íåò äðóãèõ äåéñòâèé, ïðîñòî ïðîäâèãàåìñÿ âïåðžä.
        goTo(getNextWaypoint());
    }

    
    private void initializeStrategy(Wizard self, Game game) {
        if (random == null) {
            random = new Random(game.getRandomSeed());

            double mapSize = game.getMapSize();

            waypointsByLane.put(LaneType.MIDDLE, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    random.nextBoolean()
                            ? new Point2D(600.0D, mapSize - 200.0D)
                            : new Point2D(200.0D, mapSize - 600.0D),
                    new Point2D(800.0D, mapSize - 800.0D),
                    new Point2D(mapSize - 600.0D, 600.0D)
            });

            waypointsByLane.put(LaneType.TOP, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(100.0D, mapSize - 400.0D),
                    new Point2D(200.0D, mapSize - 800.0D),
                    new Point2D(200.0D, mapSize * 0.75D),
                    new Point2D(200.0D, mapSize * 0.5D),
                    new Point2D(200.0D, mapSize * 0.25D),
                    new Point2D(200.0D, 200.0D),
                    new Point2D(mapSize * 0.25D, 200.0D),
                    new Point2D(mapSize * 0.5D, 200.0D),
                    new Point2D(mapSize * 0.75D, 200.0D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            waypointsByLane.put(LaneType.BOTTOM, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(400.0D, mapSize - 100.0D),
                    new Point2D(800.0D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.25D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.5D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.75D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.75D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.5D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.25D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            switch ((int) self.getId()) {
                case 1:
                case 2:
                case 6:
                case 7:
                    lane = LaneType.TOP;
                    break;
                case 3:
                case 8:
                    lane = LaneType.MIDDLE;
                    break;
                case 4:
                case 5:
                case 9:
                case 10:
                    lane = LaneType.BOTTOM;
                    break;
                default:
            }

            waypoints = waypointsByLane.get(lane);

        }
    }

   
    private void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }


    private Point2D getNextWaypoint() {
        int lastWaypointIndex = waypoints.length - 1;
        Point2D lastWaypoint = waypoints[lastWaypointIndex];

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    private Point2D getPreviousWaypoint() {
        Point2D firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    private void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());

        move.setTurn(angle);

        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
        }
    }


    private LivingUnit getNearestTarget() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));

        LivingUnit nearestTarget = null;
        double nearestTargetDistance = Double.MAX_VALUE;

        for (LivingUnit target : targets) {
            if (target.getFaction() == Faction.NEUTRAL || target.getFaction() == self.getFaction()) {
                continue;
            }

            double distance = self.getDistanceTo(target);

            if (distance < nearestTargetDistance) {
                nearestTarget = target;
                nearestTargetDistance = distance;
            }
        }

        return nearestTarget;
    }

    private static final class Point2D {
        private final double x;
        private final double y;

        private Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getDistanceTo(double x, double y) {
            return StrictMath.hypot(this.x - x, this.y - y);
        }

        public double getDistanceTo(Point2D point) {
            return getDistanceTo(point.x, point.y);
        }

        public double getDistanceTo(Unit unit) {
            return getDistanceTo(unit.getX(), unit.getY());
        }
    }
}
