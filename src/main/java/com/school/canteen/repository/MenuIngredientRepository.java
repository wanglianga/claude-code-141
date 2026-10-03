package com.school.canteen.repository;

import com.school.canteen.entity.MenuIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MenuIngredientRepository extends JpaRepository<MenuIngredient, Long> {

    @Query("select mi from MenuIngredient mi join mi.meal m join m.menu wm " +
            "where lower(mi.ingredientName) = lower(:ingredientName) " +
            "and wm.status in (com.school.canteen.enums.MenuStatus.PUBLISHED, com.school.canteen.enums.MenuStatus.EXECUTING) " +
            "and m.mealDate >= :fromDate order by m.mealDate")
    List<MenuIngredient> findUpcomingUsage(@Param("ingredientName") String ingredientName,
                                           @Param("fromDate") LocalDate fromDate);
}
