package se.ifmo.ru.lab1.dto;

import java.util.List;

public class RelatedObjectsResponse {
    private boolean hasCoordinates;
    private CoordinatesDTO coordinates;
    private boolean hasChapter;
    private ChapterDTO chapter;
    private List<SpaceMarineDTO> relatedSpaceMarines; // для Chapters и Coordinates
    
    public RelatedObjectsResponse() {}
    
    public RelatedObjectsResponse(boolean hasCoordinates, CoordinatesDTO coordinates, 
                                 boolean hasChapter, ChapterDTO chapter) {
        this.hasCoordinates = hasCoordinates;
        this.coordinates = coordinates;
        this.hasChapter = hasChapter;
        this.chapter = chapter;
    }
    
    public RelatedObjectsResponse(List<SpaceMarineDTO> relatedSpaceMarines) {
        this.relatedSpaceMarines = relatedSpaceMarines;
    }
    
    public boolean isHasCoordinates() {
        return hasCoordinates;
    }
    
    public void setHasCoordinates(boolean hasCoordinates) {
        this.hasCoordinates = hasCoordinates;
    }
    
    public CoordinatesDTO getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(CoordinatesDTO coordinates) {
        this.coordinates = coordinates;
    }
    
    public boolean isHasChapter() {
        return hasChapter;
    }
    
    public void setHasChapter(boolean hasChapter) {
        this.hasChapter = hasChapter;
    }
    
    public ChapterDTO getChapter() {
        return chapter;
    }
    
    public void setChapter(ChapterDTO chapter) {
        this.chapter = chapter;
    }
    
    public List<SpaceMarineDTO> getRelatedSpaceMarines() {
        return relatedSpaceMarines;
    }
    
    public void setRelatedSpaceMarines(List<SpaceMarineDTO> relatedSpaceMarines) {
        this.relatedSpaceMarines = relatedSpaceMarines;
    }
}
