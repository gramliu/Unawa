package com.gl.unawa.util;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.gl.unawa.Constants;
import com.gl.unawa.EmptyTabFactory;
import com.gl.unawa.R;
import com.gl.unawa.listeners.CVListener;
import com.gl.unawa.listeners.GestureListener;
import com.gl.unawa.listeners.STTListener;
import com.gl.unawa.nn.ImageClassifierASL;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import java.io.IOException;

public class Util_Startup {


    public static void setupGUI(final Activity activity) {

        final ImageView yellow = activity.findViewById(R.id.yellow);
        final ConstraintLayout titleBg = activity.findViewById(R.id.titleBg);
        if (Constants.STARTUP) {
            Constants.STARTUP = false;
            titleBg.setVisibility(View.VISIBLE);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    yellow.animate().setDuration(1000).rotation(120).translationX(150).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            yellow.animate().setDuration(1000).rotation(240).translationY(130).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animator) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            titleBg.animate().setDuration(500).alpha(0).setListener(new Animator.AnimatorListener() {
                                                @Override
                                                public void onAnimationStart(Animator animator) {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animator animator) {
                                                    titleBg.setVisibility(View.GONE);
                                                }

                                                @Override
                                                public void onAnimationCancel(Animator animator) {
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animator animator) {
                                                }
                                            });
                                        }
                                    }, 100);
                                }

                                @Override
                                public void onAnimationCancel(Animator animator) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animator) {

                                }
                            });
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });
                }
            }, 1000);
        }

        Constants.tabHost = activity.findViewById(R.id.tabHost);
        Constants.tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                int old = Constants.TAB;
                Log.i("MainActivity", "Tab changed!");
                switch (s) {
                    case "OCR":
                        Constants.TAB = Constants.OCR;
                        break;

                    case "Sign":
                        Constants.TAB = Constants.SIGN;
                        break;

                    case "Listen":
                        Constants.TAB = Constants.LISTEN;
                        break;

                    default:
                        Constants.TAB = Constants.OCR;
                }
                Utility.tabTransition(activity, old, Constants.TAB);
            }
        });

    }

    public static void setupConstants(final AppCompatActivity activity) {
        Constants.listenText = activity.findViewById(R.id.sttTextPreview);
        Constants.cameraBridgeViewBase = activity.findViewById(R.id.cvCameraView);
        Constants.cameraBridgeViewBase.setCvCameraViewListener(new CVListener(activity));
        Constants.cameraBridgeViewBase.setVisibility(View.VISIBLE);
        Constants.cameraBridgeViewBase.disableView();
        Constants.cameraView = activity.findViewById(R.id.surface_view);
        Constants.subtitle = activity.findViewById(R.id.subtitle);

        Constants.tabHost = activity.findViewById(R.id.tabHost);
        Constants.tabHost.setup();

        Constants.tabHost.addTab(Constants.tabHost.newTabSpec("OCR").setIndicator("OCR").setContent(new EmptyTabFactory(activity)));
        Constants.tabHost.addTab(Constants.tabHost.newTabSpec("Sign").setIndicator("Sign").setContent(new EmptyTabFactory(activity)));
        Constants.tabHost.addTab(Constants.tabHost.newTabSpec("Listen").setIndicator("Listen").setContent(new EmptyTabFactory(activity)));

        Constants.avv = activity.findViewById(R.id.visualizer);
        Constants.avv.receive(0);

        Constants.listener = new STTListener();
        Constants.recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        Constants.recognizer.setRecognitionListener(Constants.listener);
        Constants.recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        Constants.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        Constants.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        Constants.recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        Constants.recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        Constants.gestureDetector = new GestureDetector(activity, new GestureListener());

        final ImageButton listenButton = activity.findViewById(R.id.startListen);
        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utility.animateScale(listenButton);
                if (Constants.isListening) {
                    Toast.makeText(activity, "Still listening", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Utility.requestPermissions(activity);
                    Constants.isListening = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Constants.baseLoaderCallback = new BaseLoaderCallback(activity) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i("Unawa::UtilityStartup", "OpenCV loaded successfully");
                        // Load ndk built module, as specified in moduleName in build.gradle
                        // after opencv initialization
                        if (Constants.TAB == Constants.SIGN) {
                            System.loadLibrary("native-lib");
                            Constants.cameraBridgeViewBase.enableView();
                            Log.i("MainActivity", "enabled frame!");
                        }
                    }
                    break;
                    default: {
                        super.onManagerConnected(status);
                    }
                }
            }
        };

        final int[] bars = {R.id.hBar, R.id.sBar, R.id.vBar};
        for (int i = 0; i < 3; i++) {
            int bar = bars[i];
            RangeSeekBar<Integer> range = activity.findViewById(bar);
            range.setTextAboveThumbsColor(R.color.textColor);
            range.setRangeValues(0, 255);
            final int j = i;
            range.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
                @Override
                public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                    Utility.log("RangeSeekBar", "Low: " + minValue + " High: " + maxValue);
                    if (Constants.settingsMode == Constants.SETTINGS_RED) {
                        Constants.hsvBounds_Red[2 * j] = minValue;
                        Constants.hsvBounds_Red[2 * j + 1] = maxValue;
                    } else if (Constants.settingsMode == Constants.SETTINGS_GREEN) {
                        Constants.hsvBounds_Green[2 * j] = minValue;
                        Constants.hsvBounds_Green[2 * j + 1] = maxValue;
                    }
                }
            });
        }

        final ImageButton settingsButton = activity.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsButton.animate().rotation(90).setDuration(250).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        settingsButton.setRotation(0);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
                Constants.settingsMode = (Constants.settingsMode + 1) % 3;
                activity.findViewById(R.id.subtitle).setVisibility(Constants.settingsMode <= Constants.SETTINGS_OR ? View.VISIBLE : View.GONE);
                activity.findViewById(R.id.sliderContainer).setVisibility(Constants.settingsMode >= Constants.SETTINGS_GREEN ? View.VISIBLE : View.GONE);
                TextView settingsLabel = activity.findViewById(R.id.settingsLabel);
                if (Constants.settingsMode == Constants.SETTINGS_RED) {
                    settingsLabel.setText("Red Gloves Calibration");
                } else {
                    settingsLabel.setText("Green Gloves Calibration");
                }
                for (int i = 0; i < 3; i++) {
                    int bar = bars[i];
                    RangeSeekBar<Integer> range = activity.findViewById(bar);
                    int[] hsvBounds = Constants.hsvBounds_Red;
                    if (Constants.settingsMode == Constants.SETTINGS_GREEN) {
                        hsvBounds = Constants.hsvBounds_Green;
                    }
                    range.setSelectedMinValue(hsvBounds[2 * i]);
                    range.setSelectedMaxValue(hsvBounds[2 * i + 1]);
                }
            }
        });
        try {
            Constants.classifier = new ImageClassifierASL(activity);
        } catch (IOException e) {
            Log.i("Util::Startup", "Classifier was not initialized");
            e.printStackTrace();
        }

    }

}