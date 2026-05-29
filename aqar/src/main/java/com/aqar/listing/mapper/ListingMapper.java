package com.aqar.listing.mapper;

import com.aqar.listing.dto.ListingDetailResponse;
import com.aqar.listing.dto.ListingImageResponse;
import com.aqar.listing.dto.ListingSummaryResponse;
import com.aqar.listing.entity.ListingEntity;
import com.aqar.listing.entity.ListingImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ListingMapper {
    @Mapping(target = "images", source = "images")
    ListingDetailResponse toDetailResponse(ListingEntity entity);

    ListingSummaryResponse toSummaryResponse(ListingEntity entity);

    ListingImageResponse toImageResponse(ListingImageEntity entity);

    List<ListingImageResponse> toImageResponses(List<ListingImageEntity> entities);
}