package com.example.Flavor.Models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Recipe implements Serializable {

 private String id;
 private String title;
 private String description;
 private String ingredients;
 private String instructions;
 private String categoryId;
 private String userId;
 private String userEmail;
 private long timestamp;
 private boolean isSaved;

 public Recipe() {
 }

 public Recipe(String title, String description, String ingredients,
               String instructions, String categoryId, String userId, String userEmail) {
  this.title = title;
  this.description = description;
  this.ingredients = ingredients;
  this.instructions = instructions;
  this.categoryId = categoryId;
  this.userId = userId;
  this.userEmail = userEmail;
  this.timestamp = System.currentTimeMillis();
  this.isSaved = false;
 }

 // Конструктор из Map (для Firebase)
 public Recipe(Map<String, Object> map) {
  this.id = (String) map.get("id");
  this.title = (String) map.get("title");
  this.description = (String) map.get("description");
  this.ingredients = (String) map.get("ingredients");
  this.instructions = (String) map.get("instructions");
  this.categoryId = (String) map.get("categoryId");
  this.userId = (String) map.get("userId");
  this.userEmail = (String) map.get("userEmail");
  this.timestamp = (long) map.get("timestamp");
  this.isSaved = false;
 }

 // Геттеры и сеттеры
 public String getId() { return id; }
 public void setId(String id) { this.id = id; }

 public String getTitle() { return title; }
 public void setTitle(String title) { this.title = title; }

 public String getDescription() { return description; }
 public void setDescription(String description) { this.description = description; }

 public String getIngredients() { return ingredients; }
 public void setIngredients(String ingredients) { this.ingredients = ingredients; }

 public String getInstructions() { return instructions; }
 public void setInstructions(String instructions) { this.instructions = instructions; }

 public String getCategoryId() { return categoryId; }
 public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

 public String getUserId() { return userId; }
 public void setUserId(String userId) { this.userId = userId; }

 public String getUserEmail() { return userEmail; }
 public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

 public long getTimestamp() { return timestamp; }
 public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

 public boolean isSaved() { return isSaved; }
 public void setSaved(boolean saved) { isSaved = saved; }

 // Для сохранения в Firebase
 public Map<String, Object> toMap() {
  Map<String, Object> map = new HashMap<>();
  map.put("id", id);
  map.put("title", title);
  map.put("description", description);
  map.put("ingredients", ingredients);
  map.put("instructions", instructions);
  map.put("userId", userId);
  map.put("userEmail", userEmail);
  map.put("timestamp", timestamp);
  return map;
 }
}