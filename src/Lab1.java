import TSim.CommandException;
import TSim.SensorEvent;
import TSim.TSimInterface;
import java.util.concurrent.Semaphore;

public class Lab1 {

  // Semaphore for middle section (only one train at a time)
  private static final Semaphore middleSection = new Semaphore(1);

  public Lab1(int speed1, int speed2) {
    new Thread(new Train(1, speed1), "Train-1").start();
    new Thread(new Train(2, speed2), "Train-2").start();
  }

  static class Train implements Runnable {
    private final int id;
    private final int speed;
    private int dir = +1; // +1 = forward, -1 = backward

    // Track last sensor triggered globally
    private static volatile int lastX = -1;
    private static volatile int lastY = -1;

    Train(int id, int initialSpeed) {
      this.id = id;
      this.speed = Math.abs(initialSpeed);
    }

    @Override
    public void run() {
      TSimInterface tsi = TSimInterface.getInstance();
      try {
        tsi.setSpeed(id, dir * speed);
        System.out.println("Train " + id + " started at " + dir * speed);

        while (true) {
          SensorEvent e = tsi.getSensor(id);
          if (e.getStatus() != SensorEvent.ACTIVE) continue;

          int x = e.getXpos();
          int y = e.getYpos();
          System.out.printf("Train %d: sensor (%d,%d)%n", id, x, y);

          // remember last sensor globally
          lastX = x;
          lastY = y;

          // ---------------- Station handling ----------------
          if ((x == 16 && y == 5) || (x == 16 && y == 13)) {
            stopDwellReverse(tsi);
          }

          // ---------------- Middle section entry ----------------
          if ((x == 11 && y == 8) || (x == 11 && y == 7) ||
              (x == 8 && y == 9) || (x == 11 && y == 10)) {

            // If exit sensors (19,8) or (18,9) are still active  wait
            if ((lastX == 19 && lastY == 8) || (lastX == 18 && lastY == 9)) {
              tsi.setSpeed(id, 0);
              System.out.println("Train " + id + " waiting 5s: exit occupied");
              Thread.sleep(5000);
              tsi.setSpeed(id, dir * speed);
              System.out.println("Train " + id + " resuming after wait");
            }

            // Then try to acquire the section
            if (!middleSection.tryAcquire()) {
              tsi.setSpeed(id, 0);
              System.out.println("Train " + id + " waiting for middle section...");

              middleSection.acquire(); // block until free
              tsi.setSpeed(id, dir * speed);
              System.out.println("Train " + id + " entering middle section");
            } else {
              System.out.println("Train " + id + " acquired middle section");
            }
          }

          // ---------------- Middle section exit ----------------
          if ((x == 19 && y == 8) || (x == 18 && y == 9)) {
            middleSection.release();
            System.out.println("Train " + id + " released middle section");
          }

          // ---------------- Switch rules ----------------
          if (x == 14 && y == 7) {
            tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
            System.out.println("Sensor (14,7)  set (17,7) RIGHT");
          }
          if (x == 19 && y == 8) {
            tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
            System.out.println("Sensor (19,8)  set (17,7) LEFT");
          }

          if (x == 18 && y == 9) {
            tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
            System.out.println("Sensor (18,9)  set (15,9) RIGHT");
          }
          if (x == 13 && y == 9) {
            tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
            System.out.println("Sensor (13,9)  set (15,9) LEFT");
          }

          if (x == 11 && y == 10) {
            tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
            System.out.println("Sensor (11,10)  set (15,9) LEFT");
          }

          if (x == 8 && y == 9) {
            tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
            System.out.println("Sensor (8,9)  set (4,9) LEFT");
          }
          if (x == 2 && y == 9) {
            tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
            System.out.println("Sensor (2,9)  set (4,9) RIGHT");
          }

          if (x == 5 && y == 11) {
            tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
            System.out.println("Sensor (5,11)  set (3,11) LEFT");
          }
          if (x == 1 && y == 10) {
            tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
            System.out.println("Sensor (1,10)  set (3,11) RIGHT");
          }
        }

      } catch (CommandException ce) {
        System.err.println("Command failed for train " + id + ": " + ce.getMessage());
        System.exit(1);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

    private void stopDwellReverse(TSimInterface tsi) throws CommandException {
      try {
        tsi.setSpeed(id, 0);
        System.out.println("Train " + id + " stopped at station");
        Thread.sleep(2000); // dwell 2 sec
        dir = -dir;
        tsi.setSpeed(id, dir * speed);
        System.out.println("Train " + id + " leaving station at speed " + dir * speed);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
