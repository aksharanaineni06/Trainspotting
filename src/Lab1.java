import TSim.CommandException;
import TSim.SensorEvent;
import TSim.TSimInterface;
import java.util.concurrent.Semaphore;

public class Lab1 {

  private static Semaphore[] sem = new Semaphore[11];

  public Lab1(int speed1, int speed2) {
    ///declare all semaphores on section 1 trough 10
    sem[0] = new Semaphore(1);//cross section
    sem[1] = new Semaphore(1);//section 1 and 3
    sem[2] = new Semaphore(1);//aection 2 and 4
   
    sem[5] = new Semaphore(1);//critical section 5
    sem[6] = new Semaphore(1);//section 6
   
    sem[8] = new Semaphore(1);//critical section 8
    sem[9] = new Semaphore(1);//section 9
   

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
                System.out.println("- going to track 6");
                tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
              } else {
                System.out.println("- 6 busy, going to track 7");
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
         
          //track 7
          if (x == 10 && y == 10) {
            if (goingDown) {
              trackCheck(tsi, 8);
              tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
            } else {
              trackCheck(tsi, 5);
              tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
            }
          }

         
          //track 8
          if (x == 3 && y == 9) {
            if (goingDown) {
              if (sem[9].tryAcquire()) tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
              else tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
            }
            else sem[8].release();
          }

          if (x == 2 && y == 11) {
            if (goingDown) sem[8].release();
            else {
              if (sem[6].tryAcquire()) tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
              else tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
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
            if (goingDown) break;
            else {
              trackCheck(tsi, 8);
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

    private void stopDwellReverse(TSimInterface tsi) throws CommandException { //stops, waits and turns around at stations
      try {
        tsi.setSpeed(id, 0); //stop train
        Thread.sleep(2000); // dwell 2s
        dir = -dir; //changing direction
        goingDown = !goingDown; //updating variable keeping track of overall direction
        tsi.setSpeed(id, dir * speed); //starts train again
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

    private void trackCheck(TSimInterface tsi, int num) { //checks upcoming track for avalability
      try {
        System.out.println("- trackcheck " + num + " :train " + id);
        if (sem[num].tryAcquire() == false) { //track is busy, train stops and waits until available
          tsi.setSpeed(id, 0);
          System.out.println("- waiting on track " + num);
          sem[num].acquire(); //makes train wait until able to acquire needed semaphore for upcoming track
          System.out.println("- track " + num + " acquired by train " + id);
          Thread.sleep(3000); //lets the other train have a margin for passing by
          System.out.println("- going to track " + num);
          tsi.setSpeed(id, dir * speed); //train starts again
        }
        else {
            System.out.println("- acquired immediately track " + num); //if track not busy then the train goes straight on
        }
      } catch (CommandException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
