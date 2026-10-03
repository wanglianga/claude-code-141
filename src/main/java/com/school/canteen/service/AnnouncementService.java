package com.school.canteen.service;

import com.school.canteen.dto.ActionDtos.AnnouncementRequest;
import com.school.canteen.entity.ParentAnnouncement;
import com.school.canteen.enums.AnnouncementStatus;
import com.school.canteen.enums.MealType;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.ParentAnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final ParentAnnouncementRepository repository;
    private final AnomalyService anomalyService;

    public AnnouncementService(ParentAnnouncementRepository repository, AnomalyService anomalyService) {
        this.repository = repository;
        this.anomalyService = anomalyService;
    }

    public List<ParentAnnouncement> list(AnnouncementStatus status) {
        return status != null ? repository.findByStatus(status) : repository.findAll();
    }

    @Transactional
    public ParentAnnouncement create(AnnouncementRequest req, String username) {
        ParentAnnouncement pa = new ParentAnnouncement();
        pa.setTitle(req.title());
        pa.setContent(req.content());
        pa.setMealDate(req.mealDate());
        if (req.mealType() != null) {
            pa.setMealType(MealType.valueOf(req.mealType()));
        }
        pa.setDishName(req.dishName());
        if (req.anomalyId() != null) {
            pa.setAnomaly(anomalyService.get(req.anomalyId()));
        }
        pa.setCreatedBy(username);
        return repository.save(pa);
    }

    @Transactional
    public ParentAnnouncement publish(Long id) {
        ParentAnnouncement pa = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("公告不存在: " + id));
        if (pa.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw ApiException.badRequest("公告已发布");
        }
        pa.setStatus(AnnouncementStatus.PUBLISHED);
        pa.setPublishedAt(LocalDateTime.now());
        return repository.save(pa);
    }
}
