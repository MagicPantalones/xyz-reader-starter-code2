package com.example.xyzreader.remote;

import android.os.Parcel;
import android.os.Parcelable;

public class BookCover implements Parcelable{

    private int id;
    private String title;
    private String author;
    private String photo;
    private String published_date;

    public static final Parcelable.Creator<BookCover> CREATOR = new Creator<BookCover>() {
        @Override
        public BookCover createFromParcel(Parcel source) {
            return new BookCover(source);
        }

        @Override
        public BookCover[] newArray(int size) {
            return new BookCover[size];
        }
    };

    public BookCover(Parcel in) {
        this.id = (int) in.readValue(int.class.getClassLoader());
        this.title = (String) in.readValue(String.class.getClassLoader());
        this.author = (String) in.readValue(String.class.getClassLoader());
        this.photo = (String) in.readValue(String.class.getClassLoader());
        this.published_date = (String) in.readValue(String.class.getClassLoader());
    }

    public BookCover() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getPublished_date() { return published_date; }
    public void setPublished_date(String published_date) { this.published_date = published_date; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(title);
        dest.writeValue(author);
        dest.writeValue(photo);
        dest.writeValue(published_date);
    }
}
