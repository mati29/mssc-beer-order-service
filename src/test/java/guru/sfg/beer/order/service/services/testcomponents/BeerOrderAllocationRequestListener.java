package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationRequestListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void list(Message msg) {
        Boolean allocationError = false;
        Boolean pendingInventory = false;

        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();

        //conditions for allocation failure and partial allocation
        if("allocation-failure".equals(request.getBeerOrderDto().getCustomerRef())) {
            allocationError = true;
        }
        if("partial-allocation".equals(request.getBeerOrderDto().getCustomerRef())) {
            pendingInventory = true;
        }

        Boolean finalPendingInventory = pendingInventory;

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            if(finalPendingInventory) {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity()-1);
            } else {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        });

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResult.builder()
                        .allocationError(allocationError)
                        .pendingInventory(pendingInventory)
                        .beerOrderDto(request.getBeerOrderDto())
                        .build());
    }

}
