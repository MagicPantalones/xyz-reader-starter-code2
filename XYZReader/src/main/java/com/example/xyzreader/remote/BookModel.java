package com.example.xyzreader.remote;

import android.os.Parcel;
import android.os.Parcelable;

public class BookModel implements Parcelable {

    private int id;
    private String title;
    private String author;
    private String body;
    private String thumb;
    private String photo;
    private double aspect_ratio;
    private int published_date;

    public static final Parcelable.Creator<BookModel> CREATOR = new Creator<BookModel>() {
        @Override
        public BookModel createFromParcel(Parcel source) {
            return new BookModel(source);
        }

        @Override
        public BookModel[] newArray(int size) {
            return new BookModel[size];
        }
    };

    public BookModel(Parcel in) {
        this.id = (int) in.readValue(int.class.getClassLoader());
        this.title = (String) in.readValue(String.class.getClassLoader());
        this.author = (String) in.readValue(String.class.getClassLoader());
        this.body = (String) in.readValue(String.class.getClassLoader());
        this.thumb = (String) in.readValue(String.class.getClassLoader());
        this.photo = (String) in.readValue(String.class.getClassLoader());
        this.aspect_ratio = (double) in.readValue(double.class.getClassLoader());
        this.published_date = (int) in.readValue(int.class.getClassLoader());
    }

    public BookModel() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public double getAspect_ratio() { return aspect_ratio; }
    public void setAspect_ratio(double aspect_ratio) { this.aspect_ratio = aspect_ratio; }

    public int getPublished_date() { return published_date; }
    public void setPublished_date(int published_date) { this.published_date = published_date; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(title);
        dest.writeValue(author);
        dest.writeValue(body);
        dest.writeValue(thumb);
        dest.writeValue(photo);
        dest.writeValue(aspect_ratio);
        dest.writeValue(published_date);
    }
}

