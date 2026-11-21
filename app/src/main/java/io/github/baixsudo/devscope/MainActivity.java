package io.github.baixsudo.devscope;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        render();

        findViewById(R.id.btn_refresh).setOnClickListener(v -> render());
        findViewById(R.id.btn_share).setOnClickListener(v -> share());
    }

    private void render() {
        LinearLayout content = findViewById(R.id.content);
        content.removeAllViews();

        List<HardwareInfo.Section> sections = HardwareInfo.gather(this);
        for (HardwareInfo.Section section : sections) {
            content.addView(sectionHeader(section.title));
            content.addView(sectionCard(section));
        }
    }

    private TextView sectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title.toUpperCase());
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.08f);
        tv.setTextColor(getColor(R.color.accent));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(20), dp(18), dp(20), dp(6));
        tv.setLayoutParams(lp);
        return tv;
    }

    private LinearLayout sectionCard(HardwareInfo.Section section) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_background);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), 0, dp(16), 0);
        card.setLayoutParams(lp);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));

        for (int i = 0; i < section.items.size(); i++) {
            HardwareInfo.Item item = section.items.get(i);
            card.addView(buildRow(item.key, item.value));
            if (i < section.items.size() - 1) card.addView(divider());
        }
        return card;
    }

    private LinearLayout buildRow(String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView k = new TextView(this);
        k.setText(key);
        k.setTextSize(14);
        k.setTextColor(getColor(R.color.text_dim));
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        k.setLayoutParams(kp);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(14);
        v.setTextColor(getColor(R.color.text_bright));
        v.setGravity(Gravity.END);
        v.setPadding(dp(12), 0, 0, 0);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        v.setLayoutParams(vp);

        row.addView(k);
        row.addView(v);
        return row;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(getColor(R.color.divider));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        d.setLayoutParams(lp);
        return d;
    }

    private void share() {
        StringBuilder sb = new StringBuilder();
        sb.append("DevScope \u2014 device report\n");
        sb.append("========================\n");
        for (HardwareInfo.Section section : HardwareInfo.gather(this)) {
            sb.append('\n').append("[").append(section.title).append("]\n");
            for (HardwareInfo.Item item : section.items) {
                sb.append(item.key).append(": ").append(item.value).append('\n');
            }
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "DevScope device report");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "Share report"));
    }

    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }
}
