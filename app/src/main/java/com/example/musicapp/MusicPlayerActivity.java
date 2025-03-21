package com.example.musicapp;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {
    TextView titleTV, currentTimeTv, totalTimeTv;
    SeekBar seekBar;
    ImageView pausePlay, nextBtn, previousBtn, musicIcon;
    ArrayList<AudioModel> songList;
    AudioModel currentSong;
    MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    Handler handler = new Handler();
    int x = 0;

    Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            // Cập nhật SeekBar và thời gian hiện tại
            int currentPosition = mediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            currentTimeTv.setText(convertToMMSS(String.valueOf(currentPosition)));

            // Cập nhật mỗi giây
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titleTV = findViewById(R.id.song_title);
        currentTimeTv = findViewById(R.id.current_time);
        totalTimeTv = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seek_bar);
        pausePlay = findViewById(R.id.pause_play);
        nextBtn = findViewById(R.id.next);
        previousBtn = findViewById(R.id.previous);
        musicIcon = findViewById(R.id.music_icon_big);

        titleTV.setSelected(true);  // Lướt qua tiêu đề khi bài hát dài

        songList = (ArrayList<AudioModel>) getIntent().getSerializableExtra("LIST");

        setResourcesWithMusic();

        MusicPlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTimeTv.setText(convertToMMSS(mediaPlayer.getCurrentPosition() + ""));

                    if (mediaPlayer.isPlaying()) {
                        pausePlay.setImageResource(R.drawable.ic_pause);
                        musicIcon.setRotation(x++);
                    } else {
                        pausePlay.setImageResource(R.drawable.ic_play);
                        musicIcon.setRotation(0);
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    void setResourcesWithMusic() {
        currentSong = songList.get(MyMediaPlayer.currentIndex);
        titleTV.setText(currentSong.getTitle());
        totalTimeTv.setText(convertToMMSS(currentSong.getDuration()));

        pausePlay.setOnClickListener(view -> pausePlay());
        nextBtn.setOnClickListener(view -> playNextSong());
        previousBtn.setOnClickListener(view -> playPreviousSong());

        playMusic();
    }

    private void playMusic() {
        mediaPlayer.reset();  // Đảm bảo rằng MediaPlayer được reset trước khi chuẩn bị phát bài mới
        try {
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepare();  // Đảm bảo chuẩn bị MediaPlayer trước khi phát
            mediaPlayer.start();

            // Cập nhật SeekBar
            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());

            // Bắt đầu cập nhật SeekBar và thời gian
            handler.postDelayed(updateSeekBarRunnable, 1000);

            // Thiết lập listener cho việc phát xong bài hát và tự động chuyển bài
            mediaPlayer.setOnCompletionListener(mp -> playNextSong());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing the song.", Toast.LENGTH_SHORT).show();  // Thông báo lỗi nếu không thể phát bài hát
        }
    }

    private void playNextSong() {
        if (MyMediaPlayer.currentIndex == songList.size() - 1)
            return;

        MyMediaPlayer.currentIndex += 1;
        mediaPlayer.reset();
        setResourcesWithMusic();
    }

    private void playPreviousSong() {
        if (MyMediaPlayer.currentIndex == 0)
            return;

        MyMediaPlayer.currentIndex -= 1;
        mediaPlayer.reset();
        setResourcesWithMusic();
    }

    private void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            handler.removeCallbacks(updateSeekBarRunnable);  // Dừng cập nhật SeekBar
        } else {
            mediaPlayer.start();
            handler.postDelayed(updateSeekBarRunnable, 1000);  // Tiếp tục cập nhật SeekBar
        }
    }

    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();  // Giải phóng MediaPlayer khi Activity bị hủy
            handler.removeCallbacks(updateSeekBarRunnable);  // Dừng cập nhật SeekBar khi Activity bị hủy
        }
    }
}
