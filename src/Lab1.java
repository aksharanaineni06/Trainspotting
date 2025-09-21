import TSim.CommandException;
import TSim.SensorEvent;
import TSim.TSimInterface;
import java.util.concurrent.Semaphore;

public class Lab1 {

  private static Semaphore[] sem = new Semaphore[11];

  public Lab1(int speed1, int speed2) {
    ///declare semaphores on section 1 trough 10
    sem[0] = new Semaphore(1);
    sem[1] = new Semaphore(1);
    sem[2] = new Semaphore(1);
    sem[3] = new Semaphore(1);
    sem[4] = new Semaphore(1);
    sem[5] = new Semaphore(1);
    sem[6] = new Semaphore(1);
    sem[7] = new Semaphore(1);
    sem[8] = new Semaphore(1);
    sem[9] = new Semaphore(1);
    sem[10] = new Semaphore(1);

    new Thread(new Train(1, speed1, true), "Train-1").start();
    new Thread(new Train(2, speed2, false), "Train-2").start();
  }

  static class Train implements Runnable {
    private final int id;
    private final int speed;
    private int dir = +1; // +1 = forward, -1 = backward
    private boolean goingDown; // is train travelling down the map?

    private static volatile int lastX = -1;
    private static volatile int lastY = -1;

    Train(int id, int initialSpeed, boolean goingDown) {
      this.id = id;
      this.speed = Math.abs(initialSpeed);
      this.goingDown = goingDown;
    }

    @Override
    public void run() {
      TSimInterface tsi = TSimInterface.getInstance();
      try {
        if (id == 1) sem[1].tryAcquire(); // train 1 starts on track 1
        if (id == 2) sem[9].tryAcquire(); // train 2 starts on track 9

        tsi.setSpeed(id, dir * speed);
        System.out.println("Train " + id + " started at " + dir * speed);

        while (true) {
          SensorEvent e = tsi.getSensor(id);
          if (e.getStatus() != SensorEvent.ACTIVE) continue;

          int x = e.getXpos();
          int y = e.getYpos();
          System.out.printf("Train %d: sensor (%d,%d)%n", id, x, y);

          lastX = x;
          lastY = y;

          // -------- Station handling --------
          if ((x == 16 && y == 5) || (x == 16 && y == 13) || (x == 16 && y == 3) || (x == 16 && y == 11)) {
            stopDwellReverse(tsi);
            if(goingDown){
              trackCheck(tsi, 0);
              System.out.println("cross section acquired");
            }
          }

          // -------- Switch + track rules --------
          //cross section
          
          //track 1
          if (x == 16 && y == 7) {
            if (goingDown) sem[1].release();
            else sem[1].tryAcquire();
          }
          //track 2
          if (x == 16 && y == 8) {
            if (goingDown) sem[2].release();
            else sem[2].tryAcquire();
          }
          //track 3
          if (x == 13 && y == 7) {
            if (goingDown) {
              trackCheck(tsi, 5);
              tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
              sem[0].release(); //release cross section
            }
          }
          //track 4
          if (x == 13 && y == 8) {
            if (goingDown) {
              trackCheck(tsi, 5);
              tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
              sem[0].release(); //release cross section
            }
          }
          //track 5
          if (x == 18 && y == 7) {
            if (goingDown) {
              sem[5].tryAcquire();
              if (sem[6].tryAcquire()) {
                tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
              } else {
                sem[7].tryAcquire();
                tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
              }
            } else sem[5].release();
          }

          if (x == 17 && y == 9) {
            if (goingDown) {
              sem[5].release();
            } else {
              sem[5].tryAcquire();
              if (sem[1].tryAcquire()) {
                tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
              } else {
                tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
              }
            }
          }
          //track 6
          if (x == 14 && y == 9) {
            if (goingDown) sem[6].tryAcquire();
            else sem[6].release();
          }
          
          if (x == 10 && y == 9) {
            if (goingDown) {
              trackCheck(tsi, 8);
              tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
            } else {
              trackCheck(tsi, 5);
              tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
            }
          }

          if (x == 5 && y == 9) {
            if (goingDown) sem[6].release();
            else sem[6].tryAcquire();
          }
          //track 10
          if (x == 14 && y == 10) {
            if (goingDown) sem[7].tryAcquire();
            else sem[7].release();
          }

          if (x == 10 && y == 10) {
            if (goingDown) {
              trackCheck(tsi, 8);
              tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
            } else {
              trackCheck(tsi, 5);
              tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
            }
          }

          if (x == 5 && y == 10) {
            if (goingDown) sem[7].release();
            else sem[7].tryAcquire();
          }
          //track 8
          if (x == 3 && y == 9) {
            if (goingDown) {
              if (sem[9].tryAcquire()) tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
              else tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
            } else sem[8].release();
          }

          if (x == 2 && y == 11) {
            if (goingDown) sem[8].release();
            else {
              if (sem[6].tryAcquire()) tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
              else tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
            }
          }
          //track 9
          if (x == 6 && y == 11) {
            if (goingDown) sem[9].tryAcquire();
            else {
              trackCheck(tsi, 8);
              sem[9].release();
              tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
            }
          }
          //track 10
          if (x == 4 && y == 13) {
            if (goingDown) sem[10].tryAcquire();
            else {
              trackCheck(tsi, 8);
              sem[10].release();
              tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
            }
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
        Thread.sleep(2000); // dwell
        dir = -dir;
        goingDown = !goingDown;
        tsi.setSpeed(id, dir * speed);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

    private void trackCheck(TSimInterface tsi, int num) {
      try {
        if (!sem[num].tryAcquire()) {
          tsi.setSpeed(id, 0);
          sem[num].acquire();
          Thread.sleep(3000);
          tsi.setSpeed(id, dir * speed);
        }
      } catch (CommandException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
