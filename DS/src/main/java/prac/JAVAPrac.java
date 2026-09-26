/*
public class JAVAPrac {

    // FIX to SDM , SDM to UTM , UTM to TREP

    class TransactionMessage{

         Transaction transaction;
        TransactionType transactionType;
    }

    @Builder
    class Transaction {
        String security;
        public String tradeDate;
        String clientTransactionId;
    }

    enum TransactionType {
        REPO_AGREEMENT, CASH_TRADE;
    }

}

TransactionMessage transactionMessage = new TransactionMessage(new Transaction() , TransactionType.CASH_TRADE);

class MQListner {

    void consumeFromMq(){
        // consume -> FIXFormat -> String
    }
}
class FIXToSDMTransform {

    @Transform
    TransactionMessage transform(String fixMessage){
        JSONMapper mapper = ()
        TransactionMessage


    }
}

ExecuterService*/
