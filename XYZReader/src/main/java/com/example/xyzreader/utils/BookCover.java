package com.example.xyzreader.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class BookCover implements Parcelable{

    private int id;
    private String title;
    private String author;
    private String photo;
    private String thumb;
    private String publishedDate;
    private float aspectRatio;

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
        this.thumb = (String) in.readValue(String.class.getClassLoader());
        this.publishedDate = (String) in.readValue(String.class.getClassLoader());
        this.aspectRatio = (float) in.readValue(float.class.getClassLoader());
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

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }

    public float getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(float aspectRatio) { this.aspectRatio = aspectRatio; }

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
        dest.writeValue(thumb);
        dest.writeValue(publishedDate);
        dest.writeValue(aspectRatio);
    }
}
