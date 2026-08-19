package tv.withaibuild.customiuizer.subs

object SelectorResultDelivery {
    fun canDeliverFromSource(sourceIsAdded: Boolean, targetExists: Boolean): Boolean {
        return sourceIsAdded && targetExists
    }

    fun canAcceptAtBackStackTarget(targetExists: Boolean, _targetIsAdded: Boolean): Boolean {
        return targetExists
    }
}
