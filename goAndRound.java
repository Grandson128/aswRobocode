package man;

import java.awt.geom.Rectangle2D;
import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class goAndRound extends AdvancedRobot{

	int numberOfEnemys = 0; //Variable to hold number of enemy robots
	int moveDirection = 1;	//Variable to handle movement direction
	double countTurns = 0;	//Variable to keep track of turns
	double oldEnergy=100;	//Variable to hold enemy robot energy
		
	/**
	 * Function that updates numberOfEnemys still remaining
	 */
	public void getNumberOfEnemys() {
		numberOfEnemys = getOthers();
	}
	
	/**
	 * Function responsible for the robot's initial behavior
	 */
	public void initBehavior() {
		getNumberOfEnemys();	//Update number of robots alive
		
		setAdjustGunForRobotTurn(true); //Sets the gun to turn independent from the robot's turn. 
		setAdjustRadarForGunTurn(true);	//Sets the radar to turn independent from the gun's turn. 
		
		if(getRadarTurnRemaining() == 0.0) {	//Checks if radar is still
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);	//Starts rotating the radar until some robot is canned
		}
		
		setAhead(4000);
	}
	
	/**
	 * Function checks if robot is still, if true, inverts the movement direction
	 */
	public void ifRobotStopped() {
		if(getVelocity() == 0) {
			moveDirection = -moveDirection;	//Invert direction
		}
	}
	
	/**
	 *The main method in every robot.
	 */
	public void run() {
		while(true) {
			countTurns++;
			if(countTurns == 1)
				initBehavior();
			
			execute();	//Executes any actions
		}
	}
	
	/**
	 *This method is called when your robot sees another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {

		double angleToEnemy = getHeadingRadians() + e.getBearingRadians();	//angle to the scanned robot
		double distanceToEnemy = e.getDistance();	//Distance to the enemy robot
		
		/**
		 *RADAR
		 */
		double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians());	//angle to turn radar
		double extraTurn = Math.min(Math.atan(24.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);
		radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);	//increase or decrease the radar radius
		setTurnRadarRightRadians(radarTurn); //turn radar
		
		/**
		 *GUN
		 */
		double enemyLastVelocity=e.getVelocity() * Math.sin(e.getHeadingRadians() -angleToEnemy);	//enemy later velocity
		double aimTurn = robocode.util.Utils.normalRelativeAngle(angleToEnemy- getGunHeadingRadians()+enemyLastVelocity/20);	//amount to turn our gun, lead just a little bit
		setTurnGunRightRadians(aimTurn); //turn our gun
		
		
		moveAndShoot(distanceToEnemy, angleToEnemy, enemyLastVelocity);
		fintaDaMulherNua(e.getEnergy(), distanceToEnemy);
		oldEnergy=e.getEnergy();	//update enemy energy
	}
	
	/**
	 * Function to handle robot movement
	 * 
	 * @param distanceToEnemy Distance to the enemy robot
	 * @param angleToEnemy angle to the enemy robot
	 * @param enemyLastVelocity enemy later velocity
	 */
	public void moveAndShoot(double distanceToEnemy, double angleToEnemy, double enemyLastVelocity) {
		
		if(distanceToEnemy > 250) {
			setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(angleToEnemy-getHeadingRadians()+enemyLastVelocity/getVelocity()));	//drive towards the enemies predicted future location				
			setAhead((distanceToEnemy - 140)*moveDirection);	//move forward
			setFire(0.5);
		}if(distanceToEnemy <= 250 && distanceToEnemy > 150) {
			setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(angleToEnemy-getHeadingRadians()+enemyLastVelocity/getVelocity()));	//drive towards the enemies predicted future location				
			setAhead((distanceToEnemy - 140)*moveDirection);	//move forward
			setFire(1);
		}else if(distanceToEnemy <= 150) {
			setTurnLeft(-90-distanceToEnemy);	//turn perpendicular to the enemy
			setAhead((distanceToEnemy + 160)*moveDirection);	//move forward
			setFire(3);
		}
		wallsmoothing(angleToEnemy);
	}
	
	/**
	 * A method for avoiding collisions with walls without needing to reverse direction.
	 * instead of reversing direction every time the robot would hit the wall
	 * the robot will instead turn as it approaches the wall
	 * then move right along the wall.
	 * 
	 * @param angleToEnemy angle to the enemy robot
	 */
	public void wallsmoothing(double angleToEnemy) {
		
		double objective = angleToEnemy-Math.PI/2*moveDirection;
		Rectangle2D mapRectangle = new Rectangle2D.Double(18, 18, getBattleFieldWidth()-36, getBattleFieldHeight()-36);
		
		while(!mapRectangle.contains(getX()+Math.sin(objective)*120, getY()+ Math.cos(objective)*120)) {
			objective -= moveDirection*0.1;
		}
		
		double smoothturn = robocode.util.Utils.normalRelativeAngle(objective-getHeadingRadians());
		
		if (Math.abs(smoothturn) > Math.PI/2){
			smoothturn = robocode.util.Utils.normalRelativeAngle(smoothturn + Math.PI);
		}
		
		setTurnRightRadians(smoothturn);
	}
	
	/**
	 *This method is called when your robot collides with a wall.
	 *Because this method is called a lot of times in some particular cases
	 *the movement direction is only reversed when the turn is a multiple of 20
	 */
	public void onHitWall(HitWallEvent e){
		if(countTurns%20 == 0) {
			moveDirection=-moveDirection;
		}
	}
	
	/**
	 *This method is called when your robot collides with another robot.
	 *Because this method is called a lot of times in some particular cases
	 *the movement direction is only reversed when the turn is a multiple of 20
	 */
	public void onHitRobot(HitRobotEvent e) {
		if(countTurns%20 == 0) {
			moveDirection=-moveDirection;
		}
	}

	/**
	 * Function to handle movement when the enemy robot shoots
	 * Mainly randomizes movement direction every time that the enemy's robot energy decreases
	 * @param enemyEnergy	Enemy latest energy
	 * @param distanceToEnemy	Distance to the enemy
	 */
	public void fintaDaMulherNua(double enemyEnergy, double distanceToEnemy) {
		if(oldEnergy-enemyEnergy<=3&&oldEnergy-enemyEnergy>=0.1){
			if(Math.random()>.8){
				moveDirection*=-1;
			}
		}
	}

}
