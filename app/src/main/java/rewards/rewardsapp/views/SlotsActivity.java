package rewards.rewardsapp.views;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import rewards.rewardsapp.R;
import rewards.rewardsapp.models.SlotReel;
import rewards.rewardsapp.models.UserInformation;
import rewards.rewardsapp.presenters.Presenter;

public class SlotsActivity extends AppCompatActivity {

    //presenter
    private Presenter presenter;

    //arrays
    private SlotReel.ReelListener[] reelListeners;
    private ImageView[] slotImgs;
    private static int[] imageBank = {R.drawable.slots_cherry, R.drawable.slots_chili, R.drawable.slots_gold,
            R.drawable.slots_horseshoe, R.drawable.slots_moneybag}; //R.drawable.slots_bell

    //views
    private Button spin;
    private Button autoSpin;
    private TextView resultMsg;
    TextView spinsLeft;
    TextView pointsEarned;
    private boolean done;

    //constants
    private static final int SMALL_WIN = 100;
    private static final int MEDIUM_WIN = 1000;
    private static final int LARGE_WIN = 10000;
    private static final long AUTO_CLICK_WAIT = 100;

    private int spinCount, totalPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        presenter = new Presenter(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slots);
        slotImgs = new ImageView[5];
        reelListeners = new SlotReel.ReelListener[slotImgs.length];
        reelListenerSetup();
        presenter.setSlotsModel(imageBank, reelListeners);
        findViews();
        spinCount = 100;
        totalPoints = 0;
        spinsLeft.setText(Integer.toString(spinCount));
        pointsEarned.setText(Integer.toString(totalPoints));
    }

    //spins the reels and waits for them to stop spinning before checking for win
    public void spin(View view){
        if(spinCount > 0) {
            resultMsg.setText("");
            done = false;
            spin.setText("Spinning...");
            autoSpin.setText("Spinning...");
            autoSpin.setClickable(false);
            spin.setClickable(false);
            spinCount--;
            spinsLeft.setText(Integer.toString(spinCount));

            presenter.spinReels();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkWinStatus();
                    if (spinCount != 0) {
                        spin.setClickable(true);
                        spin.setText("Spin");
                        autoSpin.setClickable(true);
                        autoSpin.setText("Auto Spin");
                    } else {
                        spin.setText("NO SPINS LEFT");
                        autoSpin.setText("NO SPINS LEFT");
                        try {
                            String jsonResponse = presenter.restGet("getPointsInfo", null);
                            JSONObject pointsInfo = new JSONObject(jsonResponse);
                            resultMsg.setText("Overall points earned: " + pointsInfo.get("totalEarned").toString());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        done = true;
                    }
                }
            }, presenter.getSpinTime());
        }
    }

    //spins the slot machine until all spins are used
    public void autoSpin(View view){
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                spin.performClick();
                if(!done) clickAgain();
            }
        });
    }

    //helper method for autoSpin
    private void clickAgain(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                spin.performClick();
                if(!done) clickAgain();
            }
        }, presenter.getSpinTime() + AUTO_CLICK_WAIT);
    }

    //sets the reel listeners that are sent to the SlotsModel through the presenter
    private void reelListenerSetup(){
        for(int i = 0; i < slotImgs.length; i++) {
            final int finalI = i;
            reelListeners[i] = new SlotReel.ReelListener() {
                @Override
                public void newIcon(final int img) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            slotImgs[finalI].setImageResource(img);
                        }
                    });
                }
            };
        }

    }

    //checks with the presenter for the number of matches and processes the result
    private void checkWinStatus(){
        int winNum = presenter.checkWin();

        switch (winNum){
            case 3: resultMsg.setText("Small win. +" + SMALL_WIN + " points");
                presenter.restPut("putPointsInfo", new UserInformation(SMALL_WIN, 0, false).jsonStringify());
                totalPoints += SMALL_WIN;
                pointsEarned.setText(Integer.toString(totalPoints));
                break;
            case 4: resultMsg.setText("Medium win! +" + MEDIUM_WIN + " points");
                presenter.restPut("putPointsInfo", new UserInformation(MEDIUM_WIN, 0, false).jsonStringify());
                totalPoints += MEDIUM_WIN;
                pointsEarned.setText(Integer.toString(totalPoints));
                break;
            case 5:
                resultMsg.setText("MAJOR PRIZE!! +" + LARGE_WIN + " points");
                presenter.restPut("putPointsInfo", new UserInformation(LARGE_WIN, 0, false).jsonStringify());
                totalPoints += LARGE_WIN;
                pointsEarned.setText(Integer.toString(totalPoints));
                break;
            default: resultMsg.setText("You lose :(");
                break;
        }
    }

    private void findViews(){
        slotImgs[0] = (ImageView) findViewById(R.id.slot_1);
        slotImgs[1] = (ImageView) findViewById(R.id.slot_2);
        slotImgs[2] = (ImageView) findViewById(R.id.slot_3);
        slotImgs[3] = (ImageView) findViewById(R.id.slot_4);
        slotImgs[4] = (ImageView) findViewById(R.id.slot_5);
        spin = (Button) findViewById(R.id.spin);
        autoSpin = (Button) findViewById(R.id.auto_spin);
        resultMsg = (TextView) findViewById(R.id.win_message);
        spinsLeft = (TextView) findViewById(R.id.remaining);
        pointsEarned = (TextView) findViewById(R.id.points_won);
    }

}