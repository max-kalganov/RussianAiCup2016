import model.*;

import java.util.*;

public final class MyStrategy implements Strategy {
	private static enum state {
	    tank, support, cerry
	} 
	private static enum strategy {
	    attack, deffence
	}
	
	private static state myState;
	private static strategy myStrategy;	
	private static boolean setVariables = true;
	private static Building myBase = null; 
	private static Point2D notMyBase = null; 
	
	
	private Wizard self;
	private World world;
	private Game game;
	private Move move;
	
	//private static int deathCount = 0;
	private static boolean isNearMyBase = true;
	@Override
	public void move(Wizard self, World world, Game game, Move move) {
		InitTick(self,world,game,move);
		if(setVariables)
			SetVariables();
		Moving();
		
		Attack();
		
	}
	
	private void Moving(){	
		WithdrawalFromTheLine();	//Уход с линии
		LineChoice();				//Выбор линии
		BonusSelection();			//Подбор бонусов
		MovementDuringTheAttack();	//Движение во время атаки
		PositionChoice();			//Выбор позиции для атаки
		goTo(getNextWaypoint(),false);
	}
	
	private void Attack(){
		TargetChoice();				//Выбор цели
		AttacksStopCriterion();		//Критерий остановки атаки цели
		DerogationCriteria();		//Критерий отступления
	}
	// Block Moving
	private void LineChoice(){
		waypoints = waypointsByLane.get(lane);
	} 	
	private void BonusSelection(){}			
	private void WithdrawalFromTheLine(){
		// Если на какой-то линии продвинулись дальше , то я после смерти иду туда
		if (self.getDistanceTo(myBase)<=self.getRadius()*10 && isNearMyBase){
			isNearMyBase = false;
			LaneType lanePrev = lane;
			lane = FindTheMostDistantMinion();
			if(lane == null)
				lane = lanePrev;
		}else if(self.getDistanceTo(myBase)>self.getRadius()*10){
			isNearMyBase = true;
		}
	}
	private void MovementDuringTheAttack(){}
	private void PositionChoice(){}			
	
	// Block Attack
	private void TargetChoice(){} 			
	private void AttacksStopCriterion(){} 	
	private void DerogationCriteria(){} 	
	
	private void InitTick(Wizard self, World world, Game game, Move move){
		this.self = self;
		this.world = world;
		this.game = game;
		this.move = move;
		
		List<Building> targets1 = new ArrayList<>();
		targets1.addAll(Arrays.asList(world.getBuildings()));
		
		for (Building target : targets1) {
			if(target.getType() == BuildingType.FACTION_BASE && target.getFaction() == self.getFaction()){
				myBase = target;
				break;
			}
		}
		if(myBase.getX()==400)
		{
			notMyBase = new Point2D(3600,400);
		}else
			notMyBase = new Point2D(400,3600);
		
		initializeStrategy(self, game);

	}
	
	private void SetVariables(){
		setVariables = false;
		
		// пока это просто рандом в начале игры
		int codeForState = (new Random()).nextInt()%3; 
		switch(codeForState+1){
		case 1:
			myState = state.tank;
			break;
		case 2:
			myState = state.support;
			break;
		case 3:
			myState = state.cerry;
			break;
		}
		int codeForStrategy = (new Random()).nextInt()%2; 
		switch(codeForStrategy+1){
		case 1:
			myStrategy = strategy.attack;
			break;
		case 2:
			myStrategy = strategy.deffence;
			break;
		}
					
		
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

	// Здесь начинаются временные функции и переменные, которые в последствии будут заменены на более доработанные 
	
	private LaneType FindTheMostDistantMinion(){
		List<LivingUnit> targets2 = new ArrayList<>();
		
		targets2.addAll(Arrays.asList(world.getMinions()));

		LivingUnit nearestTarget = null;
		double nearestTargetDistance = Double.MAX_VALUE;
		
		for (LivingUnit target : targets2) {
			if (target.getFaction() == Faction.NEUTRAL || target.getFaction() != self.getFaction()) {
				continue;
			}

			double distance = notMyBase.getDistanceTo(target);
			
			if (distance < nearestTargetDistance) {
				nearestTarget = target;
				nearestTargetDistance = distance;
			}
			
		}
		if(nearestTarget == null)
			return null;
		return LaneChoice(nearestTarget);
		//return lane;
	}
	
	private LaneType LaneChoice(LivingUnit target){
		LaneType TheLane = null;
		double minDist = 4000;
		
		for(int j = 0;j<3;j++){
			LaneType now;
			if(j==0)now = LaneType.MIDDLE;
			else if(j==1)now = LaneType.TOP;
			else now = LaneType.BOTTOM;
	
			Point2D[] waypoints = waypointsByLane.get(now);
			Point2D firstWaypoint = waypoints[0];
			for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
				Point2D waypoint = waypoints[waypointIndex];
				
				if (waypoint.getDistanceTo(target) < minDist) {
					minDist = waypoint.getDistanceTo(target);
					if(j==0){
						TheLane = LaneType.MIDDLE;}
					else if(j==1)
						TheLane = LaneType.TOP;
					else TheLane = LaneType.BOTTOM;
				}
				//  - ?
				/*if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(target)) {
					if(j==0)
						return LaneType.MIDDLE;
					else if(j==1)
						return LaneType.TOP;
					return LaneType.BOTTOM;
				}*/
			}
		}
		
		return TheLane;
	}
	private static final double WAYPOINT_RADIUS = 100.0D;
	
	private Random random;
	private final Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);
	private LaneType lane = null;
	private Point2D[] waypoints;
	private void initializeStrategy(Wizard self, Game game) {
		if (random == null) {
			random = new Random(game.getRandomSeed());

			double mapSize = game.getMapSize();

			waypointsByLane.put(LaneType.MIDDLE,
					new Point2D[] { new Point2D(100.0D, mapSize - 100.0D),
							random.nextBoolean() ? new Point2D(600.0D, mapSize - 200.0D)
									: new Point2D(200.0D, mapSize - 600.0D),
							new Point2D(800.0D, mapSize - 800.0D), new Point2D(mapSize - 600.0D, 600.0D) });

			waypointsByLane.put(LaneType.TOP,
					new Point2D[] { new Point2D(100.0D, mapSize - 100.0D), new Point2D(100.0D, mapSize - 400.0D),
							new Point2D(200.0D, mapSize - 800.0D), new Point2D(200.0D, mapSize * 0.75D),
							new Point2D(200.0D, mapSize * 0.5D), new Point2D(200.0D, mapSize * 0.25D),
							new Point2D(200.0D, 200.0D), new Point2D(mapSize * 0.25D, 200.0D),
							new Point2D(mapSize * 0.5D, 200.0D), new Point2D(mapSize * 0.75D, 200.0D),
							new Point2D(mapSize - 200.0D, 200.0D) });

			waypointsByLane.put(LaneType.BOTTOM, new Point2D[] { new Point2D(100.0D, mapSize - 100.0D),
					new Point2D(400.0D, mapSize - 100.0D), new Point2D(800.0D, mapSize - 200.0D),
					new Point2D(mapSize * 0.25D, mapSize - 200.0D), new Point2D(mapSize * 0.5D, mapSize - 200.0D),
					new Point2D(mapSize * 0.75D, mapSize - 200.0D), new Point2D(mapSize - 200.0D, mapSize - 200.0D),
					new Point2D(mapSize - 200.0D, mapSize * 0.75D), new Point2D(mapSize - 200.0D, mapSize * 0.5D),
					new Point2D(mapSize - 200.0D, mapSize * 0.25D), new Point2D(mapSize - 200.0D, 200.0D) });

				}
		if(lane == null)
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

	private void goTo(Point2D point, boolean saveAngle) {
		final int maxSpeed = 54;
		double angle = self.getAngleTo(point.getX(), point.getY());
		if (saveAngle == false) {
			move.setTurn(angle);
			if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
				move.setSpeed(game.getWizardForwardSpeed());
			}
		} else {
			Double Tan = Math.tan(angle);
			if (angle == Math.PI / 2) {
				move.setStrafeSpeed(maxSpeed);
				move.setSpeed(0);
			}
			if (angle == -1* Math.PI / 2) {
				move.setStrafeSpeed(-1*maxSpeed);
				move.setSpeed(0);

			}
			
			double speedR = Math.abs(game.getWizardStrafeSpeed());
			double speedB = -1*Math.abs(game.getWizardBackwardSpeed());
			int sign = 1;
			if (angle < 0){
				sign = -1;
				angle = sign * angle;
				//angle = 2*Math.PI - angle;
			}
			if((angle>Math.atan(speedR/game.getWizardForwardSpeed()))&&(angle<3*Math.PI/4)){
				move.setStrafeSpeed(sign*speedR);
				//if(angle > Math.PI/2)
				move.setSpeed(Tan/speedR);
			}
			else{
				double speed = speedB;
				if(angle < Math.PI/2){
					speed = game.getWizardForwardSpeed();
				}
				move.setStrafeSpeed(sign*Math.abs(Tan*speed));
				move.setSpeed(speed);
			
			}
			
		}

	}

}


	
	

/*
	private static final double WAYPOINT_RADIUS = 100.0D;
	private static double LOW_HP_FACTOR = 0.5D;
	
	// константа для дистанции , если она не определена
	private final double wrongDistance = -1;
	// последнее изменение позиции
	private static double changingPosition = 0;
	// предыдущая позиция
	private static Point2D prevPos = new Point2D(0, 0);
	private static double prevLife;
	private static double damagedLife;

	
	
	
	
	private void SomeFunc(Wizard self, World world, Game game, Move move){
		if (game.getTickCount() == 1){
			damagedLife = prevLife = self.getMaxLife();
		}
		if (prevLife > self.getLife()){
			damagedLife = prevLife;
			LOW_HP_FACTOR = 0.5D;
		}else{
			if (prevLife >= damagedLife && prevLife > self.getMaxLife()*0.25D){
				LOW_HP_FACTOR = 0.25D;
				damagedLife = prevLife; 
				}
		}
		prevLife = self.getLife();
		// определяем дистанцию до ближайшей цели
		LivingUnit nearestTarget = getNearestTarget();
		double distance = wrongDistance;
		if (nearestTarget != null) {
			distance = self.getDistanceTo(nearestTarget);
			// отходим при малых хп
			if ((self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) && (distance < 600)) {
				goTo(getPreviousWaypoint(),true);
				setAttack(distance,nearestTarget);
				return;
			}
			// отходим , если противник ближе 300 (500 - дальность стрельбы)
			if ((distance != wrongDistance) && (distance <= 300)) {
				goTo(getPreviousWaypoint(),true);
				setAttack(self.getDistanceTo(getNearestTargetWithLowestHP()),getNearestTargetWithLowestHP());
				return;
			}
			// выходим из тупика
			Point2D curPos = new Point2D(self.getX(), self.getY());
			if (prevPos == curPos) {
				changingPosition++;
				if (changingPosition == 5) {
					move.setAction(ActionType.STAFF);
					goTo(getPreviousWaypoint(),false);
					return;
				}
			} else {
				changingPosition = 0;
				prevPos = curPos;
			}
			// из-за этого он дёргается
			if (distance <= 600)
				move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());
			if (getNearestTargetWithLowestHP() != null){			
				if(setAttack(self.getDistanceTo(getNearestTargetWithLowestHP()),getNearestTargetWithLowestHP()))
					return;
			}else 
				if(setAttack(distance,nearestTarget))
					return;
			
		}

		// Åñëè íåò äðóãèõ äåéñòâèé, ïðîñòî ïðîäâèãàåìñÿ âïåðžä.
		goTo(getNextWaypoint(),false);
	}
	
	private boolean setAttack(double distance,LivingUnit nearestTarget){

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
			return true;
			
		}
		return false;
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
	
	private double getPriority(LivingUnit u){
		List<LivingUnit> targetsBuildings = new ArrayList<>();
		List<LivingUnit> targetsWizards = new ArrayList<>();
		List<LivingUnit> targetsMinions = new ArrayList<>();
		
		targetsBuildings.addAll(Arrays.asList(world.getBuildings()));
		if (targetsBuildings.indexOf(u)!=-1)
			return 0.25;
		targetsWizards.addAll(Arrays.asList(world.getWizards()));
		if (targetsWizards.indexOf(u)!=-1)
			return 0.8;
		targetsMinions.addAll(Arrays.asList(world.getMinions()));
		if (targetsMinions.indexOf(u)!=-1)
			return 1;
		return 1;
	}
	
	private LivingUnit getNearestTargetWithLowestHP() {
		
		List<LivingUnit> targets = new ArrayList<>();
		targets.addAll(Arrays.asList(world.getBuildings()));
		targets.addAll(Arrays.asList(world.getWizards()));
		targets.addAll(Arrays.asList(world.getMinions()));
		
		
		LivingUnit nearestTarget = null;
		double nearestTargetHP = 100;
		double nearestTargetDistance = self.getCastRange();

		for (LivingUnit target : targets) {
			if (target.getFaction() == Faction.NEUTRAL || target.getFaction() == self.getFaction()) {
				continue;
			}
			
			double distance = self.getDistanceTo(target);
			double HP = target.getLife();//*100/target.getMaxLife();
		
			if (distance <= nearestTargetDistance && (HP*getPriority(target))<=nearestTargetHP ) {
				nearestTarget = target;
				nearestTargetHP = HP;
				//nearestTargetDistance = distance;
			}
		}

		return nearestTarget;
	}


	
/*	if (angle < Math.PI) {
				if (Tan > 0){
					if (Tan > game.getWizardStrafeSpeed()/game.getWizardForwardSpeed()){
						move.setStrafeSpeed(maxSpeed);
						move.setSpeed(Tan/game.getWizardStrafeSpeed());
					}
					else{
						move.setSpeed(maxSpeed);
						move.setStrafeSpeed(Tan*game.getWizardForwardSpeed());
					}
				}
				else{
					if (Tan > -1){
						move.setStrafeSpeed(Math.abs(Tan*game.getWizardBackwardSpeed()));
						move.setSpeed(-maxSpeed);
					}
					else{
						move.setSpeed(Math.abs(Tan/game.getWizardBackwardSpeed()));
						move.setStrafeSpeed(maxSpeed);
					}
				}
			}
			else{
				if (Tan > 0){
					if (Tan > 1){
						move.setStrafeSpeed(-1*game.getWizardStrafeSpeed());
						move.setSpeed(-1*Tan/game.getWizardStrafeSpeed());
					}
					else{
						move.setSpeed(-maxSpeed);
						move.setStrafeSpeed(Math.abs(Tan*game.getWizardBackwardSpeed())*(-1));
					}
				}
				else{
					if (Tan < game.getWizardStrafeSpeed()/game.getWizardForwardSpeed()){
						move.setStrafeSpeed(-maxSpeed);
						move.setSpeed(-1*Tan/game.getWizardStrafeSpeed());
					}
					else{
						move.setSpeed(maxSpeed);
						move.setStrafeSpeed(Tan*game.getWizardForwardSpeed());
					}
				}
			}*/