import model.*;

import java.sql.Savepoint;
import java.util.*;

import com.sun.org.apache.bcel.internal.generic.SWAP;

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
	private static int skipMoving = 0;
	private static int skipAttack = 0;
	private static boolean targetIsNear = false;
	private static LivingUnit target = null;
	private static int prevLvl = 0;
	
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
		Skills();
		
		Moving();
		
		Attack();
		
	}
	
	private void Skills(){
		if (self.getLevel()>prevLvl){
			prevLvl++;
			move.setSkillToLearn(SkillType.ADVANCED_MAGIC_MISSILE);
		}
	}
	
	
	private void Moving(){	
		if(IfNearTarget()){		// должно быть первым,чтобы отключить любую ходьбу и начать двигаться в зависимости от атаки
			skipMoving = 0; 
			goBack = false;
			nextWaypoint = null;
			targetIsNear = true;
			
		}
		if(skipMoving==0||skipMoving==1)MovementDuringTheAttack();	//Движение во время атаки(выбор дистанции до противника/цели + отступление)	
		if(skipMoving==0||skipMoving==2)WithdrawalFromTheLine();	//Уход с линии
		if(skipMoving==0||skipMoving==3)LineChoice();				//Выбор линии
		if(skipMoving==0||skipMoving==5)PositionChoice();			//Выбор позиции для атаки(выбор положения относительно окружающих юнитов)
		if(skipMoving==0||skipMoving==6||skipMoving==-1)MovementOnTheLane();		//Движение на линии
		
		/*if(skipMoving==0||skipMoving==4||skipMoving==1)*/BonusSelection();			//Подбор бонусов															
		if(skipMoving!=-1)goTo(getNextWaypoint(),targetIsNear);
	}
	
	private void Attack(){
		if(skipAttack==0||skipAttack==1)TargetChoice();			//Выбор цели						--have
		if(skipAttack==0||skipAttack==1)AttacksStopCriterion();	//Критерий остановки атаки цели	
		//if(skipAttack==0||skipAttack==1)DerogationCriteria();	//Критерий отступления				--have
		AttackSelectedTarget();
	}
	

	private void InitTick(Wizard self, World world, Game game, Move move){
		this.self = self;
		this.world = world;
		this.game = game;
		this.move = move;
		curPos = new Point2D(self.getX(), self.getY());
		
		// set LOw hp factor
		if (prevLife > self.getLife()){
			if(damagedLife <prevLife)
				damagedLife = prevLife;
			LOW_HP_FACTOR = 0.5D;
		}else{
			if (prevLife >= damagedLife && prevLife > self.getMaxLife()*0.25D){
				LOW_HP_FACTOR = 0.25D;
				damagedLife = prevLife; 
			}
		}
		prevLife = self.getLife();
		
		if(world.getTickIndex() == 1200)
			prevLife = self.getLife();
		
		// выходим из тупика
		if (prevPos.getDistanceTo(curPos)<=self.getRadius()/2) {
			notChangingPosition++;
			if (notChangingPosition == 50) {
				countDownBack=1;
			}
		} else {
			notChangingPosition = 0;
			prevPos = curPos;
		}		
	}

	private void SetVariables(){
		setVariables = false;
		// пока это просто рандом в начале игры
		int codeForState = (new Random()).nextInt()%3; 
		
		initializeStrategy(self, game);
		
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
		
		//set my Base & not my base
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
		
		
		// set prevLife & damaged Life
		damagedLife = prevLife = self.getMaxLife();
		
	}
	
	// Block Moving
	private void LineChoice(){
		waypoints = waypointsByLane.get(lane);
	} 	
	private boolean IfNearTarget(){
		LivingUnit nearestTarget = getNearestTarget();
		if(posWhenMetTarget == null && nearestTarget!= null)
			posWhenMetTarget = curPos;
		else if(nearestTarget == null)
			posWhenMetTarget =  null;
		if(nearestTarget == null){
			targetIsNear = false;
		}
		else{
			targetIsNear = true;
		}
		return targetIsNear;
	}
	private void BonusSelection(){
		if (self.getDistanceTo(myBase)<=self.getRadius()*10){
			took = true;
			goBack = false;
			skipMoving = 0;
			nextWaypoint = null;
	
		}
		
		if(goBack==true&&took==true){
			if(curPos.getDistanceTo(nextWaypoint)<200){
				goBack = false;
				skipMoving = 0;
				nextWaypoint = null;
			}
		}
		if((took==false)||((curPos.getX()+400>=curPos.getY())&&(curPos.getX()-400<=curPos.getY()))){
			if(took==false||(game.getBonusAppearanceIntervalTicks() - world.getTickIndex()%game.getBonusAppearanceIntervalTicks())<=200){
				took=false;
				skipMoving = 4;
				Point2D bonus = null;
				if(curPos.getDistanceTo(new Point2D(1200,1200))<curPos.getDistanceTo(new Point2D(2800,2800)) )
					bonus = new Point2D(1200,1200);
				else bonus = new Point2D(2800,2800);
				
				nextWaypoint = bonus;
				
				if(curPos.getDistanceTo(bonus)<=game.getBonusRadius()){
					took = true;
					goBack= true;
					//skipMoving = 4;
					SetLaneAndWaypoint(); // функция для задания линии и точки 
				}
				return;
			}
		}
	}		
	
	private void WithdrawalFromTheLine(){
		// Если на какой-то линии продвинулись дальше , то я после смерти иду туда
		if (self.getDistanceTo(myBase)<=self.getRadius()*10 && isNearMyBase){
			isNearMyBase = false;
			LaneType lanePrev = lane;
			lane = FindTheMostDistantMinion(1);
			if(lane == null)
				lane = lanePrev;
		}else if(self.getDistanceTo(myBase)>self.getRadius()*10){
			isNearMyBase = true;
		}
	}
	private void MovementDuringTheAttack(){
		LivingUnit nearestTarget = getNearestTarget();
		double distance;
		if(nearestTarget == null){
			skipMoving = 0;
			targetIsNear = false;
			nextWaypoint = null;
			return;
		}
		targetIsNear = true;
		skipMoving = 1;	
		nextWaypoint = posWhenMetTarget; 
		distance = self.getDistanceTo(nearestTarget);
		// отходить если враг ближе 200
		if (distance <= 200) {
			nextWaypoint = getPreviousWaypoint();
		}
		//else nextWaypoint = null;
		
		// отходим при малых хп
		if ((self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) && (distance < 600)) {
			nextWaypoint = getPreviousWaypoint();
		}
		
		
	}
	private void PositionChoice(){}		
	private void MovementOnTheLane(){
		
		if(game.getFactionMinionAppearanceIntervalTicks()-50>world.getTickIndex()){
			nextWaypoint = new Point2D(100,3500);
			
			skipMoving = -1;
			return ;
		}else{
			nextWaypoint = null;
			skipMoving = 0;
		}
		
		if(curPos.getDistanceTo(notMyBase)<=900 && getNotMyBaseHP()>300){
			//nextWaypoint = getPreviousWaypoint();
			skipMoving = -1;
		}
		else {
			nextWaypoint = null;
		}
	}
	
	// Block Attack
	private void TargetChoice(){
		target = getNearestTargetWithLowestHP();
		if(target == null)
			target = getNearestTarget();	
	} 			
	private void AttacksStopCriterion(){} 	
	private void DerogationCriteria(){} 	
	private void AttackSelectedTarget(){
		if (target!=null) {
			// узнаём угол до врага
			double angle = self.getAngleTo(target);
			// поворачиваемся
			move.setTurn(angle);
			if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
				move.setAction(ActionType.MAGIC_MISSILE);
				move.setCastAngle(angle);
				move.setMinCastDistance(self.getDistanceTo(target) - target.getRadius() + game.getMagicMissileRadius());
			}				
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
	private void SetLaneAndWaypoint(){
		LaneType laneAsVariant = FindTheMostDistantMinion(1);
		if ( curPos.getDistanceTo(new Point2D(1200,1200))
			<curPos.getDistanceTo(new Point2D(2800,2800))){
			if(laneAsVariant== LaneType.BOTTOM){
				lane = FindTheMostDistantMinion(2);
			}
			else{ 
		 		lane = laneAsVariant;
		 	}
		}else{
		 	if(laneAsVariant== LaneType.TOP)
		 		lane = FindTheMostDistantMinion(2);
		 	else lane = laneAsVariant;
		}
		int point = 200;
		if (lane == LaneType.MIDDLE)
			point = 2000;
	 	else if (lane == LaneType.BOTTOM)
	 		point = 3400;
			 	
		Point2D p = new Point2D(point,point);	
		nextWaypoint = p;
	}
	
	private double getNotMyBaseHP(){
		List<Building> targets = new ArrayList<>();
		targets.addAll(Arrays.asList(world.getBuildings()));
		for (Building target : targets) {
			if(target.getX() == notMyBase.x && target.getY() == notMyBase.y)
				return target.getLife();
		}
		return Double.MAX_VALUE;
			
	}
	
	// Здесь начинаются временные функции и переменные, которые в последствии будут заменены на более доработанные 
	
		private LaneType FindTheMostDistantMinion(int num){
		List<LivingUnit> targets2 = new ArrayList<>();
		targets2.addAll(Arrays.asList(world.getMinions()));
		
		LivingUnit nearestTarget1 = null;
		LivingUnit nearestTarget2 = null;
		LaneType resLane1 = LaneType.MIDDLE;
		LaneType resLane2 = LaneType.MIDDLE;
		double nearestTargetDistance1 = Double.MAX_VALUE;
		double nearestTargetDistance2 = Double.MAX_VALUE;
		
		
		for (LivingUnit target : targets2) {
			if (target.getFaction() == Faction.NEUTRAL || target.getFaction() != self.getFaction()) {
				continue;
			}

			double distance = notMyBase.getDistanceTo(target);
			

			if (distance < nearestTargetDistance1) {
				LaneType l;
				if((l = LaneChoice(target))!=resLane1){
					nearestTarget2 = nearestTarget1;
					resLane2 = resLane1;
					nearestTargetDistance2 = nearestTargetDistance1;
					resLane1 = l;
				}
				nearestTarget1 = target;
				nearestTargetDistance1 = distance;
				continue;
			}
			
			if (distance < nearestTargetDistance2 && resLane1 != LaneChoice(target)) {
				resLane2 = LaneChoice(target);
				nearestTarget2 = target;
				nearestTargetDistance2 = distance;
			}			
		}
		if(nearestTarget1 == null)
			return null;
		if(num == 1)
			return resLane1;
		return resLane2;
	}
	private Point2D curPos;
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
	private final static Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);
	private LaneType lane = null;
	private static Point2D[] waypoints;
	private static Point2D nextWaypoint = null; 
	private static int sign  = -1;
	private static int sign2  = -1;
	private static boolean took = true;
	private static boolean goBack = false;
	private static double LOW_HP_FACTOR = 0.5D;
	private static double prevLife;
	private static double damagedLife;
	// последнее изменение позиции
	private static double notChangingPosition = 0;
		
	
	private static int countDownBack = 0;
	private static Point2D prevPos = new Point2D(0, 0);
	
	
	private static Point2D posWhenMetTarget = null;
	
	
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
		if (nextWaypoint != null)
			return nextWaypoint;
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
	
	private void goTo(Point2D point, boolean saveAngle) {
		final int maxSpeed = 54;
		double angle = self.getAngleTo(point.getX(), point.getY());
		
		if(posWhenMetTarget==null && countDownBack != 0 && countDownBack<500){
			if(countDownBack%20 ==0){
				sign = (new Random()).nextInt(Math.abs((int)System.currentTimeMillis()))%2 == 0?-1:1;
				sign2 = (new Random()).nextInt(Math.abs((int)System.currentTimeMillis()))%2 == 0?-1:1;
			}
			
			move.setStrafeSpeed(3*sign);
			move.setSpeed(3*sign2);
			countDownBack++;
			if(notChangingPosition <50)
				countDownBack=0;
			return ;
		}
		
		
		
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
		if(	  (nearestTargetDistance<=600 && Arrays.asList(world.getBuildings()).indexOf(nearestTarget)!=-1)
			||(nearestTargetDistance<=500 && Arrays.asList(world.getBuildings()).indexOf(nearestTarget)==-1)){
			return nearestTarget;
		}
		return null;
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


}