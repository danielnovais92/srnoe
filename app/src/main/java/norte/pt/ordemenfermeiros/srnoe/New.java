package norte.pt.ordemenfermeiros.srnoe;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class New implements Comparable<New> {

    private final String url, image, title, from, content;
    private final Boolean clicked, favourite, recent;
    private final Date date;

    public New(String title, String url, String content, Date date, String image,
               String from, Boolean clicked, Boolean favourite, Boolean recent) {
        this.title = title;
        this.url = url;
        this.content = content;
        this.date = date;
        this.image = image;
        this.from = from;
        this.clicked = clicked;
        this.favourite = favourite;
        this.recent = recent;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }

    private Date getDate() {
        return date;
    }

    public String getImage() {
        return image;
    }

    public String getFrom() {
        return from;
    }

    public Boolean getClicked() {
        return clicked;
    }

    public Boolean getFavourite() {
        return favourite;
    }

    public Boolean isRecent() {
        return recent;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        return  sdf.format(getDate());
    }

    @Override
    public int compareTo(@NonNull New another) {
        return another.getDate().compareTo(this.getDate());
    }
}
