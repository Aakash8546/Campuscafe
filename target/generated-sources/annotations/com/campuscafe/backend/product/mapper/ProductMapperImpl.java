package com.campuscafe.backend.product.mapper;

import com.campuscafe.backend.category.mapper.CategoryMapper;
import com.campuscafe.backend.domain.product.Category;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.product.dto.ProductResponse;
import com.campuscafe.backend.product.dto.ProductSummaryResponse;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-24T01:12:09+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public ProductResponse toResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductResponse.ProductResponseBuilder productResponse = ProductResponse.builder();

        productResponse.id( product.getId() );
        productResponse.name( product.getName() );
        productResponse.description( product.getDescription() );
        productResponse.price( product.getPrice() );
        productResponse.imageUrl( product.getImageUrl() );
        productResponse.available( product.getAvailable() );
        productResponse.category( categoryMapper.toResponse( product.getCategory() ) );
        productResponse.createdAt( product.getCreatedAt() );
        productResponse.updatedAt( product.getUpdatedAt() );

        return productResponse.build();
    }

    @Override
    public ProductSummaryResponse toSummaryResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductSummaryResponse.ProductSummaryResponseBuilder productSummaryResponse = ProductSummaryResponse.builder();

        productSummaryResponse.categoryName( productCategoryName( product ) );
        productSummaryResponse.id( product.getId() );
        productSummaryResponse.name( product.getName() );
        productSummaryResponse.price( product.getPrice() );
        productSummaryResponse.imageUrl( product.getImageUrl() );
        productSummaryResponse.available( product.getAvailable() );

        return productSummaryResponse.build();
    }

    private String productCategoryName(Product product) {
        if ( product == null ) {
            return null;
        }
        Category category = product.getCategory();
        if ( category == null ) {
            return null;
        }
        String name = category.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
